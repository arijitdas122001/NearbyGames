package com.gameconnect.profile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.gameconnect.TestcontainersConfiguration;
import com.gameconnect.auth.dto.RegisterRequest;
import com.gameconnect.security.JwtService;

import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    // -- GET /api/users/me ----------------------------------------------

    @Test
    void getOwnProfile_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void getOwnProfile_returns200WithProfileAndZeroStats() throws Exception {
        String token = registerAndToken("own@example.com", "Own User");

        mockMvc.perform(get("/api/users/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Own User"))
                .andExpect(jsonPath("$.skillLevel").value("BEGINNER"))
                .andExpect(jsonPath("$.position").doesNotExist())
                .andExpect(jsonPath("$.stats.matchesPlayed").value(0))
                .andExpect(jsonPath("$.stats.matchesCompleted").value(0))
                .andExpect(jsonPath("$.stats.attendanceRate").doesNotExist())
                .andExpect(jsonPath("$.stats.averageRating").doesNotExist());
    }

    // -- GET /api/users/{id} ----------------------------------------------

    @Test
    void getUserProfile_existingUser_returns200() throws Exception {
        String targetToken = registerAndToken("target@example.com", "Target User");
        String targetId = userIdForToken(targetToken);

        String viewerToken = registerAndToken("viewer@example.com", "Viewer User");

        mockMvc.perform(get("/api/users/{id}", targetId)
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", viewerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetId))
                .andExpect(jsonPath("$.displayName").value("Target User"))
                .andExpect(jsonPath("$.stats.matchesPlayed").value(0));
    }

    @Test
    void getUserProfile_nonExistentUser_returns404() throws Exception {
        String viewerToken = registerAndToken("ghost-viewer@example.com", "Ghost Viewer");

        mockMvc.perform(get("/api/users/{id}", "00000000-0000-0000-0000-000000000000")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", viewerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void getUserProfile_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/users/{id}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());
    }

    // -- PATCH /api/users/me ----------------------------------------------

    @Test
    void updateProfile_partialUpdate_preservesOtherFields() throws Exception {
        String token = registerAndToken("partial@example.com", "Original Name");

        mockMvc.perform(patch("/api/users/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\": \"Just a bio\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Original Name"))
                .andExpect(jsonPath("$.bio").value("Just a bio"))
                .andExpect(jsonPath("$.skillLevel").value("BEGINNER"))
                .andExpect(jsonPath("$.position").doesNotExist());
    }

    @Test
    void updateProfile_validDisplayName_returns200() throws Exception {
        String token = registerAndToken("dname@example.com", "Old Name");

        mockMvc.perform(patch("/api/users/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\": \"New Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("New Name"));
    }

    @Test
    void updateProfile_validBio_returns200() throws Exception {
        String token = registerAndToken("bio@example.com", "Bio User");

        mockMvc.perform(patch("/api/users/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\": \"Football lover\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Football lover"));
    }

    @Test
    void updateProfile_validSkillLevel_returns200() throws Exception {
        String token = registerAndToken("skill@example.com", "Skill User");

        mockMvc.perform(patch("/api/users/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillLevel\": \"INTERMEDIATE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skillLevel").value("INTERMEDIATE"));
    }

    @Test
    void updateProfile_validProfileImageUrl_returns200() throws Exception {
        String token = registerAndToken("image@example.com", "Image User");

        mockMvc.perform(patch("/api/users/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileImageUrl\": \"https://example.com/avatar.png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").value("https://example.com/avatar.png"));
    }

    @Test
    void updateProfile_validPosition_returns200() throws Exception {
        String token = registerAndToken("pos@example.com", "Position User");

        mockMvc.perform(patch("/api/users/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"position\": \"STRIKER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value("STRIKER"));
    }

    @Test
    void updateProfile_blankDisplayName_returns400() throws Exception {
        String token = registerAndToken("blank@example.com", "Blank User");

        mockMvc.perform(patch("/api/users/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateProfile_longDisplayName_returns400() throws Exception {
        String token = registerAndToken("long@example.com", "Long User");

        String longName = "a".repeat(81);
        mockMvc.perform(patch("/api/users/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\": \"" + longName + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateProfile_longBio_returns400() throws Exception {
        String token = registerAndToken("longbio@example.com", "Long Bio User");

        String longBio = "b".repeat(2001);
        mockMvc.perform(patch("/api/users/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\": \"" + longBio + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateProfile_invalidSkillLevel_returns400() throws Exception {
        String token = registerAndToken("badskill@example.com", "Bad Skill User");

        mockMvc.perform(patch("/api/users/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillLevel\": \"PROFESSIONAL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_BODY"));
    }

    @Test
    void updateProfile_invalidPosition_returns400() throws Exception {
        String token = registerAndToken("badpos@example.com", "Bad Position User");

        mockMvc.perform(patch("/api/users/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"position\": \"COACH\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_BODY"));
    }

    @Test
    void updateProfile_emptyBody_returns400() throws Exception {
        String token = registerAndToken("emptybody@example.com", "Empty Body User");

        mockMvc.perform(patch("/api/users/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateProfile_withoutAuth_returns401() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\": \"no auth\"}"))
                .andExpect(status().isUnauthorized());
    }

    // -- helpers ----------------------------------------------------------

    private String registerAndToken(String email, String displayName) throws Exception {
        RegisterRequest request = new RegisterRequest(email, "password123", displayName);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        String responseBody = result.getResponse().getContentAsString();
        String userId = objectMapper.readTree(responseBody).get("id").asText();
        return jwtService.generateToken(userId, email);
    }

    private String userIdForToken(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", token)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }
}