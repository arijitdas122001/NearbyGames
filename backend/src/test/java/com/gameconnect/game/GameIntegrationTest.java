package com.gameconnect.game;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.gameconnect.TestcontainersConfiguration;
import com.gameconnect.auth.dto.RegisterRequest;
import com.gameconnect.auth.entity.User.SkillLevel;
import com.gameconnect.game.dto.CreateGameRequest;
import com.gameconnect.game.entity.Game;
import com.gameconnect.game.entity.Game.GameFormat;
import com.gameconnect.game.entity.MatchParticipant;
import com.gameconnect.game.repository.GameRepository;
import com.gameconnect.game.repository.MatchParticipantRepository;
import com.gameconnect.security.JwtService;

import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class GameIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JwtService jwtService;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    MatchParticipantRepository matchParticipantRepository;

    private record AuthSession(String token, UUID userId) {
    }

    private AuthSession registerAndAuth(String email, String displayName) throws Exception {
        RegisterRequest request = new RegisterRequest(email, "password123", displayName);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        String userId = objectMapper.readTree(body).get("id").asText();
        String token = jwtService.generateToken(userId, email);
        return new AuthSession(token, UUID.fromString(userId));
    }

    private CreateGameRequest validRequest() {
        return new CreateGameRequest(
                "Turf Arena",
                "MG Road, Bengaluru",
                new BigDecimal("12.9716"),
                new BigDecimal("77.5946"),
                LocalDate.now().plusDays(3),
                Instant.now().plusSeconds(7200),
                Instant.now().plusSeconds(10800),
                GameFormat.FIVE_V5,
                SkillLevel.BEGINNER,
                10,
                5,
                0,
                "Weekend pickup game");
    }

    private Map<String, Object> validBodyMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("turfName", "Turf Arena");
        m.put("turfAddress", "MG Road, Bengaluru");
        m.put("latitude", new BigDecimal("12.9716"));
        m.put("longitude", new BigDecimal("77.5946"));
        m.put("gameDate", LocalDate.now().plusDays(3).toString());
        m.put("startTime", Instant.now().plusSeconds(7200).toString());
        m.put("endTime", Instant.now().plusSeconds(10800).toString());
        m.put("format", "5V5");
        m.put("skillLevel", "BEGINNER");
        m.put("maximumPlayers", 10);
        m.put("requiredPlayers", 5);
        m.put("joiningFee", 0);
        m.put("description", "Test");
        return m;
    }

    private ResultActions postCreate(String token, CreateGameRequest request) throws Exception {
        return mockMvc.perform(post("/api/games")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions postCreateRaw(String token, String json) throws Exception {
        return mockMvc.perform(post("/api/games")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    // ── happy path ───────────────────────────────────────────────────────

    @Test
    void createGame_validRequest_returns201AndExpectedDto() throws Exception {
        AuthSession auth = registerAndAuth("owner1@example.com", "Owner 1");
        CreateGameRequest request = validRequest();

        MvcResult result = mockMvc.perform(post("/api/games")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", auth.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.ownerId").value(auth.userId.toString()))
                .andExpect(jsonPath("$.turfName").value("Turf Arena"))
                .andExpect(jsonPath("$.turfAddress").value("MG Road, Bengaluru"))
                .andExpect(jsonPath("$.latitude").value(12.9716))
                .andExpect(jsonPath("$.longitude").value(77.5946))
                .andExpect(jsonPath("$.gameDate").value(LocalDate.now().plusDays(3).toString()))
                .andExpect(jsonPath("$.startTime").exists())
                .andExpect(jsonPath("$.endTime").exists())
                .andExpect(jsonPath("$.format").value("5V5"))
                .andExpect(jsonPath("$.skillLevel").value("BEGINNER"))
                .andExpect(jsonPath("$.maximumPlayers").value(10))
                .andExpect(jsonPath("$.requiredPlayers").value(5))
                .andExpect(jsonPath("$.joiningFee").value(0))
                .andExpect(jsonPath("$.description").value("Weekend pickup game"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        String gameId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
        Game saved = gameRepository.findById(UUID.fromString(gameId)).orElseThrow();
        assert saved.getStatus() == Game.GameStatus.OPEN;
        assert saved.getOwnerId().equals(auth.userId);

        var participants = matchParticipantRepository.findByGameId(saved.getId());
        assert participants.size() == 1;
        assert participants.getFirst().getRole() == MatchParticipant.Role.OWNER;
        assert participants.getFirst().getUserId().equals(auth.userId);
        assert participants.getFirst().getAttended() == null;
    }

    @Test
    void createGame_persistsOwnerParticipantExactlyOnce() throws Exception {
        AuthSession auth = registerAndAuth("atomic@example.com", "Atomic User");
        MvcResult result = postCreate(auth.token, validRequest()).andReturn();
        String gameId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
        assert matchParticipantRepository.countByGameId(UUID.fromString(gameId)) == 1;
    }

    @Test
    void createGame_trimmedTurfFields_returnsTrimmedValues() throws Exception {
        AuthSession auth = registerAndAuth("trim@example.com", "Trim User");
        CreateGameRequest req = new CreateGameRequest(
                "  Turf Arena  ",
                "  MG Road  ",
                new BigDecimal("12.9716"),
                new BigDecimal("77.5946"),
                LocalDate.now().plusDays(3),
                Instant.now().plusSeconds(7200),
                Instant.now().plusSeconds(10800),
                GameFormat.FIVE_V5,
                SkillLevel.BEGINNER,
                10,
                5,
                0,
                "Test");

        mockMvc.perform(post("/api/games")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", auth.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.turfName").value("Turf Arena"))
                .andExpect(jsonPath("$.turfAddress").value("MG Road"));
    }

    // ── auth ─────────────────────────────────────────────────────────────

    @Test
    void createGame_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ── bean validation: missing / blank fields ──────────────────────────

    @Test
    void createGame_missingFields_returns400() throws Exception {
        AuthSession auth = registerAndAuth("missing@example.com", "Missing User");
        postCreateRaw(auth.token, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createGame_blankTurfName_returns400() throws Exception {
        AuthSession auth = registerAndAuth("blankname@example.com", "Blank Name");
        CreateGameRequest req = new CreateGameRequest(
                "   ",
                "MG Road",
                new BigDecimal("12.9716"),
                new BigDecimal("77.5946"),
                LocalDate.now().plusDays(3),
                Instant.now().plusSeconds(7200),
                Instant.now().plusSeconds(10800),
                GameFormat.FIVE_V5,
                SkillLevel.BEGINNER,
                10,
                5,
                0,
                null);
        postCreate(auth.token, req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createGame_blankTurfAddress_returns400() throws Exception {
        AuthSession auth = registerAndAuth("blankaddr@example.com", "Blank Addr");
        CreateGameRequest req = new CreateGameRequest(
                "Turf",
                "   ",
                new BigDecimal("12.9716"),
                new BigDecimal("77.5946"),
                LocalDate.now().plusDays(3),
                Instant.now().plusSeconds(7200),
                Instant.now().plusSeconds(10800),
                GameFormat.FIVE_V5,
                SkillLevel.BEGINNER,
                10,
                5,
                0,
                null);
        postCreate(auth.token, req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── bean validation: coordinates ─────────────────────────────────────

    @Test
    void createGame_invalidLatitude_returns400() throws Exception {
        AuthSession auth = registerAndAuth("badlat@example.com", "Bad Lat");
        Map<String, Object> body = validBodyMap();
        body.put("latitude", new BigDecimal("91.0"));
        postCreateRaw(auth.token, objectMapper.writeValueAsString(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createGame_invalidLongitude_returns400() throws Exception {
        AuthSession auth = registerAndAuth("badlng@example.com", "Bad Lng");
        Map<String, Object> body = validBodyMap();
        body.put("longitude", new BigDecimal("181.0"));
        postCreateRaw(auth.token, objectMapper.writeValueAsString(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── bean validation: capacity ────────────────────────────────────────

    @Test
    void createGame_maxPlayersBelowMin_returns400() throws Exception {
        AuthSession auth = registerAndAuth("lowmax@example.com", "Low Max");
        Map<String, Object> body = validBodyMap();
        body.put("maximumPlayers", 4);
        postCreateRaw(auth.token, objectMapper.writeValueAsString(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createGame_maxPlayersAboveMax_returns400() throws Exception {
        AuthSession auth = registerAndAuth("highmax@example.com", "High Max");
        Map<String, Object> body = validBodyMap();
        body.put("maximumPlayers", 23);
        postCreateRaw(auth.token, objectMapper.writeValueAsString(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createGame_requiredPlayersAboveMax_returns400() throws Exception {
        AuthSession auth = registerAndAuth("highreq@example.com", "High Req");
        Map<String, Object> body = validBodyMap();
        body.put("requiredPlayers", 11);
        postCreateRaw(auth.token, objectMapper.writeValueAsString(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── bean validation: joining fee ─────────────────────────────────────

    @Test
    void createGame_negativeJoiningFee_returns400() throws Exception {
        AuthSession auth = registerAndAuth("negfee@example.com", "Neg Fee");
        Map<String, Object> body = validBodyMap();
        body.put("joiningFee", -1);
        postCreateRaw(auth.token, objectMapper.writeValueAsString(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── service validation: scheduling ───────────────────────────────────

    @Test
    void createGame_pastGameDate_returns400() throws Exception {
        AuthSession auth = registerAndAuth("pastdate@example.com", "Past Date");
        CreateGameRequest req = new CreateGameRequest(
                "Turf",
                "Addr",
                new BigDecimal("12.9716"),
                new BigDecimal("77.5946"),
                LocalDate.now().minusDays(1),
                Instant.now().plusSeconds(7200),
                Instant.now().plusSeconds(10800),
                GameFormat.FIVE_V5,
                SkillLevel.BEGINNER,
                10,
                5,
                0,
                null);
        postCreate(auth.token, req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createGame_pastStartTime_returns400() throws Exception {
        AuthSession auth = registerAndAuth("paststart@example.com", "Past Start");
        CreateGameRequest req = new CreateGameRequest(
                "Turf",
                "Addr",
                new BigDecimal("12.9716"),
                new BigDecimal("77.5946"),
                LocalDate.now().plusDays(3),
                Instant.now().minusSeconds(3600),
                Instant.now().plusSeconds(3600),
                GameFormat.FIVE_V5,
                SkillLevel.BEGINNER,
                10,
                5,
                0,
                null);
        postCreate(auth.token, req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createGame_endTimeNotAfterStartTime_returns400() throws Exception {
        AuthSession auth = registerAndAuth("badeq@example.com", "Bad Eq");
        Instant same = Instant.now().plusSeconds(7200);
        CreateGameRequest req = new CreateGameRequest(
                "Turf",
                "Addr",
                new BigDecimal("12.9716"),
                new BigDecimal("77.5946"),
                LocalDate.now().plusDays(3),
                same,
                same,
                GameFormat.FIVE_V5,
                SkillLevel.BEGINNER,
                10,
                5,
                0,
                null);
        postCreate(auth.token, req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── invalid enums ────────────────────────────────────────────────────

    @Test
    void createGame_invalidFormat_returns400() throws Exception {
        AuthSession auth = registerAndAuth("g4-badfmt@example.com", "Bad Format");
        Map<String, Object> body = validBodyMap();
        body.put("format", "10V10");
        postCreateRaw(auth.token, objectMapper.writeValueAsString(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_BODY"));
    }

    @Test
    void createGame_invalidSkillLevel_returns400() throws Exception {
        AuthSession auth = registerAndAuth("g4-badskill@example.com", "Bad Skill");
        Map<String, Object> body = validBodyMap();
        body.put("skillLevel", "PROFESSIONAL");
        postCreateRaw(auth.token, objectMapper.writeValueAsString(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_BODY"));
    }

    // ── security: owner always from context ──────────────────────────────

    @Test
    void createGame_ignoresSpoofedOwnerId_returnsOwnerFromAuth() throws Exception {
        AuthSession auth = registerAndAuth("secure@example.com", "Secure User");
        Map<String, Object> body = validBodyMap();
        body.put("ownerId", UUID.randomUUID().toString());
        MvcResult result = postCreateRaw(auth.token, objectMapper.writeValueAsString(body))
                .andExpect(status().isCreated())
                .andReturn();
        String ownerId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("ownerId").asText();
        assert ownerId.equals(auth.userId.toString());
    }

    // ── description too long ─────────────────────────────────────────────

    @Test
    void createGame_descriptionTooLong_returns400() throws Exception {
        AuthSession auth = registerAndAuth("longdesc@example.com", "Long Desc");
        Map<String, Object> body = validBodyMap();
        body.put("description", "x".repeat(2001));
        postCreateRaw(auth.token, objectMapper.writeValueAsString(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}