package com.example.memberplatform;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/member-platform-test.db")
@AutoConfigureMockMvc
class MemberPlatformApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM members");
    }

    @Test
    void completeMemberFlow() throws Exception {
        register("andy01", "Andy", "password123");
        register("amy001", "Amy Chen", "password456");

        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM members WHERE username = ?",
                String.class,
                "andy01");
        org.hamcrest.MatcherAssert.assertThat(passwordHash, startsWith("$2"));

        HttpSession session = login("andy01", "password123");

        mockMvc.perform(get("/api/members/me").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("andy01"));

        mockMvc.perform(get("/api/members")
                        .session((org.springframework.mock.web.MockHttpSession) session)
                        .param("field", "name")
                        .param("keyword", "Amy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("amy001"));

        mockMvc.perform(put("/api/members/me")
                        .session((org.springframework.mock.web.MockHttpSession) session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"andy01\",\"name\":\"Andy Yu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.name").value("Andy Yu"))
                .andExpect(jsonPath("$.reLoginRequired").value(false));

        mockMvc.perform(delete("/api/members/me")
                        .session((org.springframework.mock.web.MockHttpSession) session)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM members WHERE username = ?", Integer.class, "andy01");
        org.junit.jupiter.api.Assertions.assertEquals(0, count);
    }

    @Test
    void rejectsDuplicateUsernameIgnoringCase() throws Exception {
        register("member01", "First", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"MEMBER01","name":"Second",
                                 "password":"password456","confirmPassword":"password456"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void protectsWriteOperations() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"member01","name":"Member",
                                 "password":"password123","confirmPassword":"password123"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/members/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"member01\",\"name\":\"Member\"}"))
                .andExpect(status().isUnauthorized());
    }

    private void register(String username, String name, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","name":"%s",
                                 "password":"%s","confirmPassword":"%s"}
                                """.formatted(username, name, password, password)))
                .andExpect(status().isCreated());
    }

    private HttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isOk())
                .andReturn();
        return result.getRequest().getSession(false);
    }
}
