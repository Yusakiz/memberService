package com.example.memberplatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class MemberManagementIntegrationTests {

    private static final Path TEST_DATABASE = Path.of(
            "target", "test-data", "management-" + UUID.randomUUID() + ".db");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("app.database.path", TEST_DATABASE::toString);
    }

    @BeforeEach
    void clearMembers() {
        jdbcTemplate.update("DELETE FROM members");
    }

    @Test
    void searchesAndUpdatesOnlyTheAuthenticatedMember() throws Exception {
        register("member_a", "會員甲", "Pass!123");
        register("member_b", "會員乙%", "Pass!123");
        long memberBId = jdbcTemplate.queryForObject(
                "SELECT id FROM members WHERE username = 'member_b'", Long.class);

        MockHttpSession session = login("member_a", "Pass!123", true);
        CsrfSession csrf = getCsrf(session);
        mockMvc.perform(put("/api/members/me")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": %d,
                                  "username": "member_a",
                                  "nickname": "新暱稱"
                                }
                                """.formatted(memberBId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reLoginRequired").value(false))
                .andExpect(jsonPath("$.member.nickname").value("新暱稱"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT nickname FROM members WHERE id = ?", String.class, memberBId))
                .isEqualTo("會員乙%");

        mockMvc.perform(get("/api/members")
                        .session(session)
                        .queryParam("field", "nickname")
                        .queryParam("keyword", "%")
                        .queryParam("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].username").value("member_b"));

        mockMvc.perform(put("/api/members/me")
                        .session(session)
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"member_new","nickname":"新暱稱"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reLoginRequired").value(true));

        login("member_a", "Pass!123", false);
        login("member_new", "Pass!123", true);
    }

    @Test
    void changesPasswordAndDeletesMemberAcrossExistingSessions() throws Exception {
        register("member_a", "會員甲", "Pass!123");
        register("member_b", "會員乙", "Pass!123");

        MockHttpSession firstSession = login("member_a", "Pass!123", true);
        MockHttpSession secondSession = login("member_a", "Pass!123", true);
        CsrfSession passwordCsrf = getCsrf(firstSession);

        mockMvc.perform(put("/api/members/me/password")
                        .session(firstSession)
                        .header(passwordCsrf.headerName(), passwordCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword":"wrong-password",
                                  "newPassword":"NewPass!456",
                                  "confirmPassword":"NewPass!456"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.currentPassword").value("目前密碼錯誤"));

        mockMvc.perform(put("/api/members/me/password")
                        .session(firstSession)
                        .header(passwordCsrf.headerName(), passwordCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword":"Pass!123",
                                  "newPassword":"NewPass!456",
                                  "confirmPassword":"NewPass!456"
                                }
                                """))
                .andExpect(status().isNoContent());

        login("member_a", "Pass!123", false);
        MockHttpSession deleteSession = login("member_a", "NewPass!456", true);
        CsrfSession deleteCsrf = getCsrf(deleteSession);
        mockMvc.perform(delete("/api/members/me")
                        .session(deleteSession)
                        .header(deleteCsrf.headerName(), deleteCsrf.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/members/me").session(secondSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("登入狀態已失效"));

        MockHttpSession memberBSession = login("member_b", "Pass!123", true);
        mockMvc.perform(get("/api/members/me").session(memberBSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("member_b"));
    }

    @Test
    void paginatesAndTreatsLikeCharactersAsLiterals() throws Exception {
        register("searcher", "查詢者", "Pass!123");
        String hash = passwordEncoder.encode("Pass!123");
        for (int index = 0; index < 25; index++) {
            jdbcTemplate.update(
                    "INSERT INTO members (username, nickname, password_hash) VALUES (?, ?, ?)",
                    "page_" + index,
                    "Page " + index,
                    hash);
        }
        jdbcTemplate.update(
                "INSERT INTO members (username, nickname, password_hash) VALUES (?, ?, ?)",
                "plainuser",
                "No underscore",
                hash);

        MockHttpSession session = login("searcher", "Pass!123", true);
        mockMvc.perform(get("/api/members")
                        .session(session)
                        .queryParam("field", "username")
                        .queryParam("keyword", "_")
                        .queryParam("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(20))
                .andExpect(jsonPath("$.hasNext").value(true));

        mockMvc.perform(get("/api/members")
                        .session(session)
                        .queryParam("field", "username")
                        .queryParam("keyword", "_")
                        .queryParam("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(5))
                .andExpect(jsonPath("$.hasNext").value(false));

        mockMvc.perform(get("/api/members")
                        .session(session)
                        .queryParam("field", "username")
                        .queryParam("keyword", "' OR 1=1 --"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    private void register(String username, String nickname, String password) throws Exception {
        CsrfSession csrf = getCsrf(null);
        mockMvc.perform(post("/api/auth/register")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "nickname":"%s",
                                  "password":"%s",
                                  "confirmPassword":"%s"
                                }
                                """.formatted(username, nickname, password, password)))
                .andExpect(status().isCreated());
    }

    private MockHttpSession login(String username, String password, boolean shouldSucceed) throws Exception {
        CsrfSession csrf = getCsrf(null);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", username)
                        .param("password", password))
                .andExpect(shouldSucceed ? status().isOk() : status().isUnauthorized())
                .andReturn();
        return shouldSucceed ? (MockHttpSession) result.getRequest().getSession(false) : null;
    }

    private CsrfSession getCsrf(MockHttpSession existingSession) throws Exception {
        var request = get("/api/auth/csrf");
        if (existingSession != null) {
            request.session(existingSession);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        return new CsrfSession(
                (MockHttpSession) result.getRequest().getSession(false),
                JsonTestValue.read(result, "token"),
                JsonTestValue.read(result, "headerName"));
    }

    private record CsrfSession(MockHttpSession session, String token, String headerName) {
    }
}
