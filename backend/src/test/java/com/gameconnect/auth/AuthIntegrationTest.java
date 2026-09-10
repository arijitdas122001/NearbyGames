package com.gameconnect.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import com.gameconnect.TestcontainersConfiguration;
import com.gameconnect.auth.dto.LoginRequest;
import com.gameconnect.auth.dto.RegisterRequest;
import com.gameconnect.security.JwtService;

import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    // ── Registration ──────────────────────────────────────────

    @Test
    void register_withValidData_returns200AndCookie() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "Test User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(cookie().exists("auth_token"))
                .andExpect(cookie().httpOnly("auth_token", true))
                .andExpect(cookie().path("auth_token", "/"))
                .andExpect(cookie().sameSite("auth_token", "Lax"));
    }

    @Test
    void register_withDuplicateEmail_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest("dup@example.com", "password123", "User One");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        RegisterRequest duplicate = new RegisterRequest("dup@example.com", "password456", "User Two");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }

    @Test
    void register_withInvalidEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("not-an-email", "password123", "Test User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void register_withShortPassword_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "ab", "Test User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void register_withBlankDisplayName_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "  ");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void register_withMissingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── Login ─────────────────────────────────────────────────

    @Test
    void login_withValidCredentials_returns200AndCookie() throws Exception {
        RegisterRequest reg = new RegisterRequest("login@example.com", "password123", "Login User");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk());

        LoginRequest login = new LoginRequest("login@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andExpect(jsonPath("$.displayName").value("Login User"))
                .andExpect(cookie().exists("auth_token"))
                .andExpect(cookie().httpOnly("auth_token", true))
                .andExpect(cookie().path("auth_token", "/"))
                .andExpect(cookie().sameSite("auth_token", "Lax"));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        RegisterRequest reg = new RegisterRequest("wrong@example.com", "password123", "Wrong User");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk());

        LoginRequest login = new LoginRequest("wrong@example.com", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_withNonExistentEmail_returns401() throws Exception {
        LoginRequest login = new LoginRequest("nonexistent@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_withInvalidEmailFormat_returns400() throws Exception {
        LoginRequest login = new LoginRequest("not-an-email", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── GET /api/auth/me ──────────────────────────────────────

    @Test
    void me_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void me_withValidJwt_returns200AndUserProfile() throws Exception {
        RegisterRequest reg = new RegisterRequest("me@example.com", "password123", "Me User");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = regResult.getResponse().getContentAsString();
        String userId = objectMapper.readTree(responseBody).get("id").asText();

        String token = jwtService.generateToken(userId, "me@example.com");

        mockMvc.perform(get("/api/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.displayName").value("Me User"))
                .andExpect(jsonPath("$.skillLevel").value("BEGINNER"))
                .andExpect(jsonPath("$.id").value(userId));
    }

    @Test
    void me_withExpiredJwt_returns401() throws Exception {
        RegisterRequest reg = new RegisterRequest("expired@example.com", "password123", "Expired User");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = regResult.getResponse().getContentAsString();
        String userId = objectMapper.readTree(responseBody).get("id").asText();

        com.gameconnect.security.JwtService expiredJwtService =
                new com.gameconnect.security.JwtService(
                        "myDefaultDevSecretKeyMustBeAtLeast32BytesLongForHs256", 0);

        String expiredToken = expiredJwtService.generateToken(userId, "expired@example.com");

        mockMvc.perform(get("/api/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", expiredToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void me_withInvalidJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", "totally.invalid.token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // ── Logout ────────────────────────────────────────────────

    @Test
    void logout_clearsCookie() throws Exception {
        RegisterRequest reg = new RegisterRequest("logout@example.com", "password123", "Logout User");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie authCookie = regResult.getResponse().getCookie("auth_token");

        mockMvc.perform(post("/api/auth/logout").cookie(authCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("auth_token"))
                .andExpect(cookie().maxAge("auth_token", 0));
    }

    // ── Public endpoint ───────────────────────────────────────

    @Test
    void health_withoutAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // ── Protected endpoint without auth ───────────────────────

    @Test
    void protectedEndpoint_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void protectedEndpoint_withValidJwt_returns200() throws Exception {
        RegisterRequest reg = new RegisterRequest("protected@example.com", "password123", "Protected User");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = regResult.getResponse().getContentAsString();
        String userId = objectMapper.readTree(responseBody).get("id").asText();

        String token = jwtService.generateToken(userId, "protected@example.com");

        mockMvc.perform(get("/api/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("protected@example.com"));
    }

    // ── Cookie security attributes ────────────────────────────

    @Test
    void register_setsCookieWithSecureFalseByDefault() throws Exception {
        RegisterRequest request = new RegisterRequest("cookie@example.com", "password123", "Cookie User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("auth_token"))
                .andExpect(cookie().httpOnly("auth_token", true))
                .andExpect(cookie().path("auth_token", "/"))
                .andExpect(cookie().sameSite("auth_token", "Lax"));
    }
}
