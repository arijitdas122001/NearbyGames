package com.gameconnect.game;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.gameconnect.TestcontainersConfiguration;
import com.gameconnect.auth.dto.RegisterRequest;
import com.gameconnect.auth.entity.User.SkillLevel;
import com.gameconnect.game.entity.Game;
import com.gameconnect.game.entity.Game.GameFormat;
import com.gameconnect.game.entity.Game.GameStatus;
import com.gameconnect.game.entity.MatchParticipant;
import com.gameconnect.game.repository.GameRepository;
import com.gameconnect.game.repository.MatchParticipantRepository;
import com.gameconnect.security.JwtService;

import tools.jackson.databind.ObjectMapper;

/**
 * Attendance tracking API (Phase 7). Uses direct DB inserts for games that
 * need past/future times or non-OPEN statuses, since the create-game API
 * requires a future start time.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AttendanceIntegrationTest {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JwtService jwtService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    MatchParticipantRepository matchParticipantRepository;

    private record AuthSession(String token, UUID userId) {
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM player_rating");
        jdbcTemplate.execute("DELETE FROM match_participant");
        jdbcTemplate.execute("DELETE FROM join_request");
        jdbcTemplate.execute("DELETE FROM game");
        jdbcTemplate.execute("DELETE FROM app_user");
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

    private jakarta.servlet.http.Cookie authCookie(String token) {
        return new jakarta.servlet.http.Cookie("auth_token", token);
    }

    private void sendCreateJoinRequest(AuthSession player, String gameId) throws Exception {
        mockMvc.perform(post("/api/games/{gameId}/join-requests", gameId)
                        .cookie(authCookie(player.token)))
                .andExpect(status().isCreated());
    }

    private void decideJoinRequest(AuthSession owner, String gameId, String requestId, String action)
            throws Exception {
        mockMvc.perform(patch("/api/games/{gameId}/join-requests/{requestId}", gameId, requestId)
                        .cookie(authCookie(owner.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"" + action + "\"}"))
                .andExpect(status().isOk());
    }

    private String createFutureGame(AuthSession owner) throws Exception {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        var request = new com.gameconnect.game.dto.CreateGameRequest(
                "Roster Turf",
                "MG Road",
                new BigDecimal("12.9716"),
                new BigDecimal("77.5946"),
                LocalDate.ofInstant(start, INDIA),
                start,
                start.plus(2, ChronoUnit.HOURS),
                GameFormat.FIVE_V5,
                SkillLevel.BEGINNER,
                10,
                null,
                0,
                null);
        MvcResult result = mockMvc.perform(post("/api/games")
                        .cookie(authCookie(owner.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private UUID insertGame(UUID ownerId, Instant start, Instant end, GameStatus status) {
        Game game = new Game();
        game.setOwnerId(ownerId);
        game.setTurfName("Turf");
        game.setTurfAddress("Addr");
        game.setLatitude(new BigDecimal("12.9716"));
        game.setLongitude(new BigDecimal("77.5946"));
        game.setGameDate(LocalDate.ofInstant(end, INDIA));
        game.setStartTime(start);
        game.setEndTime(end);
        game.setFormat(GameFormat.FIVE_V5);
        game.setSkillLevel(SkillLevel.BEGINNER);
        game.setMaximumPlayers(10);
        game.setRequiredPlayers(null);
        game.setJoiningFee(0);
        game.setDescription(null);
        game.setStatus(status);
        return gameRepository.save(game).getId();
    }

    private void insertParticipant(UUID gameId, UUID userId, MatchParticipant.Role role,
                                   Boolean attended) {
        MatchParticipant participant = new MatchParticipant();
        participant.setGameId(gameId);
        participant.setUserId(userId);
        participant.setRole(role);
        participant.setAttended(attended);
        matchParticipantRepository.save(participant);
    }

    private String rosterParticipantId(String gameId, String userId) {
        return matchParticipantRepository.findByGameId(UUID.fromString(gameId))
                .stream()
                .filter(p -> p.getUserId().equals(UUID.fromString(userId)))
                .map(MatchParticipant::getId)
                .findFirst()
                .orElseThrow()
                .toString();
    }

    // ── roster listing ──────────────────────────────────────────────────

    @Test
    void roster_includesOnlyOwnerAndAcceptedPlayers_excludesPending() throws Exception {
        AuthSession owner = registerAndAuth("r-owner@example.com", "Roster Owner");
        AuthSession accepted1 = registerAndAuth("r-accepted1@example.com", "Accepted One");
        AuthSession accepted2 = registerAndAuth("r-accepted2@example.com", "Accepted Two");
        AuthSession pending = registerAndAuth("r-pending@example.com", "Pending Player");

        String gameId = createFutureGame(owner);

        sendCreateJoinRequest(accepted1, gameId);
        sendCreateJoinRequest(accepted2, gameId);
        sendCreateJoinRequest(pending, gameId);

        var details = acceptAllFromApi(owner, gameId, 3);
        // accept the first two requests, leave the third pending
        decideJoinRequest(owner, gameId, details.get(0).id().toString(), "ACCEPT");
        decideJoinRequest(owner, gameId, details.get(1).id().toString(), "ACCEPT");

        mockMvc.perform(get("/api/games/{gameId}/participants", gameId)
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[0].userId").value(owner.userId.toString()))
                .andExpect(jsonPath("$[1].role").value("PLAYER"))
                .andExpect(jsonPath("$[1].userId").value(accepted1.userId.toString()))
                .andExpect(jsonPath("$[2].role").value("PLAYER"))
                .andExpect(jsonPath("$[2].userId").value(accepted2.userId.toString()));
    }

    private java.util.List<com.gameconnect.game.dto.JoinRequestDetailResponse> acceptAllFromApi(
            AuthSession owner, String gameId, int expectedCount) throws Exception {
        // fetch the join requests as JSON via the owner API
        MvcResult result = mockMvc.perform(get("/api/games/{gameId}/join-requests", gameId)
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andReturn();
        return java.util.Arrays.asList(objectMapper.readValue(
                result.getResponse().getContentAsString(),
                com.gameconnect.game.dto.JoinRequestDetailResponse[].class));
    }

    @Test
    void roster_nonOwner_returns403() throws Exception {
        AuthSession owner = registerAndAuth("nr-owner@example.com", "NR Owner");
        AuthSession stranger = registerAndAuth("nr-stranger@example.com", "Stranger");
        String gameId = createFutureGame(owner);

        mockMvc.perform(get("/api/games/{gameId}/participants", gameId)
                        .cookie(authCookie(stranger.token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void roster_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/games/{id}/participants", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roster_unknownGame_returns404() throws Exception {
        AuthSession owner = registerAndAuth("rg-owner@example.com", "RG Owner");
        mockMvc.perform(get("/api/games/{id}/participants", UUID.randomUUID())
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_NOT_FOUND"));
    }

    // ── attendance marking ───────────────────────────────────────────────

    @Test
    void attendance_ownerMarksAttendedTrueThenFalse() throws Exception {
        AuthSession owner = registerAndAuth("a-owner@example.com", "A Owner");
        AuthSession player = registerAndAuth("a-player@example.com", "A Player");
        Instant start = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(1, ChronoUnit.HOURS);
        UUID gameId = insertGame(owner.userId, start, end, GameStatus.OPEN);
        insertParticipant(gameId, owner.userId, MatchParticipant.Role.OWNER, null);
        insertParticipant(gameId, player.userId, MatchParticipant.Role.PLAYER, null);
        UUID hostRow = matchParticipantRepository.findByGameId(gameId).stream()
                .filter(p -> p.getUserId().equals(owner.userId)).findFirst().orElseThrow().getId();
        String playerRow = rosterParticipantId(gameId.toString(), player.userId.toString());

        mockMvc.perform(patch("/api/games/{gameId}/participants/{participantId}/attendance", gameId, playerRow)
                        .cookie(authCookie(owner.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attended\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantId").value(playerRow))
                .andExpect(jsonPath("$.attended").value(true));

        mockMvc.perform(patch("/api/games/{gameId}/participants/{participantId}/attendance", gameId, hostRow)
                        .cookie(authCookie(owner.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attended\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attended").value(false));
    }

    @Test
    void attendance_ownerCanMarkWithinGraceAfterEndTime() throws Exception {
        AuthSession owner = registerAndAuth("g-owner@example.com", "G Owner");
        AuthSession player = registerAndAuth("g-player@example.com", "G Player");
        Instant start = Instant.now().minus(3, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(30, ChronoUnit.MINUTES);
        UUID gameId = insertGame(owner.userId, start, end, GameStatus.OPEN);
        insertParticipant(gameId, player.userId, MatchParticipant.Role.PLAYER, null);
        String playerRow = rosterParticipantId(gameId.toString(), player.userId.toString());

        mockMvc.perform(patch("/api/games/{gameId}/participants/{participantId}/attendance", gameId, playerRow)
                        .cookie(authCookie(owner.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attended\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attended").value(true));
    }

    @Test
    void attendance_nonOwner_returns403() throws Exception {
        AuthSession owner = registerAndAuth("no-owner@example.com", "NO Owner");
        AuthSession stranger = registerAndAuth("no-stranger@example.com", "NO Stranger");
        Instant start = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(1, ChronoUnit.HOURS);
        UUID gameId = insertGame(owner.userId, start, end, GameStatus.OPEN);
        insertParticipant(gameId, stranger.userId, MatchParticipant.Role.PLAYER, null);
        String strangerRow = rosterParticipantId(gameId.toString(), stranger.userId.toString());

        mockMvc.perform(patch("/api/games/{gameId}/participants/{participantId}/attendance", gameId, strangerRow)
                        .cookie(authCookie(stranger.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attended\": true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void attendance_participantFromAnotherGame_returns404() throws Exception {
        AuthSession owner = registerAndAuth("c-owner@example.com", "C Owner");
        AuthSession player = registerAndAuth("c-player@example.com", "C Player");
        Instant start = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(1, ChronoUnit.HOURS);
        UUID gameId = insertGame(owner.userId, start, end, GameStatus.OPEN);
        UUID otherGameId = insertGame(owner.userId, start, end, GameStatus.OPEN);
        insertParticipant(gameId, player.userId, MatchParticipant.Role.PLAYER, null);
        // participant row belongs to the OTHER game
        UUID otherParticipantId = insertParticipantRow(otherGameId, player.userId);

        mockMvc.perform(patch("/api/games/{gameId}/participants/{participantId}/attendance", gameId, otherParticipantId)
                        .cookie(authCookie(owner.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attended\": true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_NOT_FOUND"));
    }

    private UUID insertParticipantRow(UUID gameId, UUID userId) {
        MatchParticipant participant = new MatchParticipant();
        participant.setGameId(gameId);
        participant.setUserId(userId);
        participant.setRole(MatchParticipant.Role.PLAYER);
        participant.setAttended(null);
        return matchParticipantRepository.save(participant).getId();
    }

    @Test
    void attendance_unknownGame_returns404() throws Exception {
        AuthSession owner = registerAndAuth("au-owner@example.com", "AU Owner");
        mockMvc.perform(patch("/api/games/{gameId}/participants/{participantId}/attendance",
                        UUID.randomUUID(), UUID.randomUUID())
                        .cookie(authCookie(owner.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attended\": true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_NOT_FOUND"));
    }

    @Test
    void attendance_beforeStartTime_returns409_gameNotStarted() throws Exception {
        AuthSession owner = registerAndAuth("ns-owner@example.com", "NS Owner");
        AuthSession player = registerAndAuth("ns-player@example.com", "NS Player");
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(2, ChronoUnit.HOURS);
        UUID gameId = insertGame(owner.userId, start, end, GameStatus.OPEN);
        insertParticipant(gameId, player.userId, MatchParticipant.Role.PLAYER, null);
        String playerRow = rosterParticipantId(gameId.toString(), player.userId.toString());

        mockMvc.perform(patch("/api/games/{gameId}/participants/{participantId}/attendance", gameId, playerRow)
                        .cookie(authCookie(owner.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attended\": true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_NOT_STARTED"));
    }

    @Test
    void attendance_afterCompletion_returns409_gameAlreadyCompleted() throws Exception {
        AuthSession owner = registerAndAuth("ac-owner@example.com", "AC Owner");
        AuthSession player = registerAndAuth("ac-player@example.com", "AC Player");
        Instant start = Instant.now().minus(3, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(2, ChronoUnit.HOURS);
        UUID gameId = insertGame(owner.userId, start, end, GameStatus.COMPLETED);
        insertParticipant(gameId, player.userId, MatchParticipant.Role.PLAYER, null);
        String playerRow = rosterParticipantId(gameId.toString(), player.userId.toString());

        mockMvc.perform(patch("/api/games/{gameId}/participants/{participantId}/attendance", gameId, playerRow)
                        .cookie(authCookie(owner.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attended\": true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_ALREADY_COMPLETED"));
    }

    @Test
    void attendance_cancelledGame_returns409_gameNotActive() throws Exception {
        AuthSession owner = registerAndAuth("na-owner@example.com", "NA Owner");
        AuthSession player = registerAndAuth("na-player@example.com", "NA Player");
        Instant start = Instant.now().minus(3, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(2, ChronoUnit.HOURS);
        UUID gameId = insertGame(owner.userId, start, end, GameStatus.CANCELLED);
        insertParticipant(gameId, player.userId, MatchParticipant.Role.PLAYER, null);
        String playerRow = rosterParticipantId(gameId.toString(), player.userId.toString());

        mockMvc.perform(patch("/api/games/{gameId}/participants/{participantId}/attendance", gameId, playerRow)
                        .cookie(authCookie(owner.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attended\": true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_NOT_ACTIVE"));
    }

    @Test
    void attendance_missingAttendedField_returns400() throws Exception {
        AuthSession owner = registerAndAuth("mv-owner@example.com", "MV Owner");
        AuthSession player = registerAndAuth("mv-player@example.com", "MV Player");
        Instant start = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(1, ChronoUnit.HOURS);
        UUID gameId = insertGame(owner.userId, start, end, GameStatus.OPEN);
        insertParticipant(gameId, player.userId, MatchParticipant.Role.PLAYER, null);
        String playerRow = rosterParticipantId(gameId.toString(), player.userId.toString());

        mockMvc.perform(patch("/api/games/{gameId}/participants/{participantId}/attendance", gameId, playerRow)
                        .cookie(authCookie(owner.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── game detail myParticipation ──────────────────────────────────────

    @Test
    void detail_myParticipation_presentForParticipant_absentForNonParticipant() throws Exception {
        AuthSession owner = registerAndAuth("mp-owner@example.com", "MP Owner");
        AuthSession participant = registerAndAuth("mp-participant@example.com", "MP Participant");
        AuthSession outsider = registerAndAuth("mp-outsider@example.com", "MP Outsider");
        String gameId = createFutureGame(owner);
        sendCreateJoinRequest(participant, gameId);
        var requests = acceptAllFromApi(owner, gameId, 1);
        decideJoinRequest(owner, gameId, requests.get(0).id().toString(), "ACCEPT");

        mockMvc.perform(get("/api/games/{id}", gameId).cookie(authCookie(participant.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myParticipation.role").value("PLAYER"))
                .andExpect(jsonPath("$.myParticipation.attended").doesNotExist());

        mockMvc.perform(get("/api/games/{id}", gameId).cookie(authCookie(outsider.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myParticipation").doesNotExist());
    }
}