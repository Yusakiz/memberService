package com.example.memberplatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class MemberApiIntegrationTests {

    private static final Path TEST_DATABASE = Path.of(
            "target", "test-data", "api-" + UUID.randomUUID() + ".db");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("app.database.path", TEST_DATABASE::toString);
    }

    @BeforeEach
    void clearMembers() {
        jdbcTemplate.update("DELETE FROM members");
    }

    @Test
    void registerLoginReadProfileAndLogout() throws Exception {
        CsrfSession anonymous = getCsrf(null);

        mockMvc.perform(post("/api/auth/register")
                        .session(anonymous.session())
                        .header(anonymous.headerName(), anonymous.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "  Yu_01  ",
                                  "nickname": " 慕語 ",
                                  "password": "Pass!234",
                                  "confirmPassword": "Pass!234"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("yu_01"))
                .andExpect(jsonPath("$.nickname").value("慕語"));

        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM members WHERE username = ?",
                String.class,
                "yu_01");
        assertThat(passwordHash).startsWith("$2").doesNotContain("Pass!234");

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .session(anonymous.session())
                        .header(anonymous.headerName(), anonymous.token())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "YU_01")
                        .param("password", "Pass!234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("yu_01"))
                .andReturn();

        MockHttpSession authenticatedSession = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/api/members/me").session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("慕語"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        CsrfSession authenticated = getCsrf(authenticatedSession);
        mockMvc.perform(post("/api/auth/logout")
                        .session(authenticated.session())
                        .header(authenticated.headerName(), authenticated.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/members/me").session(authenticated.session()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDuplicateUsernameIgnoringCaseAndInvalidPassword() throws Exception {
        CsrfSession csrf = getCsrf(null);
        register(csrf, "Member_1", "第一位", "Valid!123")
                .andExpect(status().isCreated());
        register(csrf, "member_1", "第二位", "Valid!123")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.username").value("帳號已被使用"));

        mockMvc.perform(post("/api/auth/register")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "member_2",
                                  "nickname": "第二位",
                                  "password": "含中文密碼12345",
                                  "confirmPassword": "含中文密碼12345"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void rejectsMissingCsrfAndBadCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        CsrfSession csrf = getCsrf(null);
        mockMvc.perform(post("/api/auth/login")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "missing")
                        .param("password", "Wrong!123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("帳號或密碼錯誤"));
    }

    private org.springframework.test.web.servlet.ResultActions register(
            CsrfSession csrf, String username, String nickname, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .session(csrf.session())
                .header(csrf.headerName(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "username": "%s",
                          "nickname": "%s",
                          "password": "%s",
                          "confirmPassword": "%s"
                        }
                        """.formatted(username, nickname, password, password)));
    }

    private CsrfSession getCsrf(MockHttpSession existingSession) throws Exception {
        var request = get("/api/auth/csrf");
        if (existingSession != null) {
            request.session(existingSession);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andReturn();
        String token = JsonTestValue.read(result, "token");
        String headerName = JsonTestValue.read(result, "headerName");
        HttpSession session = result.getRequest().getSession(false);
        return new CsrfSession((MockHttpSession) session, token, headerName);
    }

    private record CsrfSession(MockHttpSession session, String token, String headerName) {
    }
}
