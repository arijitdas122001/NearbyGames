package com.gameconnect.game;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
import com.gameconnect.game.dto.CreateGameRequest;
import com.gameconnect.game.entity.Game;
import com.gameconnect.game.entity.Game.GameFormat;
import com.gameconnect.game.entity.Game.GameStatus;
import com.gameconnect.game.entity.JoinRequest;
import com.gameconnect.game.entity.JoinRequest.RequestStatus;
import com.gameconnect.game.entity.MatchParticipant;
import com.gameconnect.game.repository.GameRepository;
import com.gameconnect.game.repository.JoinRequestRepository;
import com.gameconnect.game.repository.MatchParticipantRepository;
import com.gameconnect.security.JwtService;

import tools.jackson.databind.ObjectMapper;

/**
 * Join-request rule matrix (Phase 11): the create/get/decide endpoints and the
 * conflict codes they emit, plus a concurrency test proving the pessimistic
 * game-row lock admits exactly one winner for the final slot.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class JoinRequestIntegrationTest {

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

    @Autowired
    JoinRequestRepository joinRequestRepository;

    private record AuthSession(String token, UUID userId) {
    }

    private record DecideOutcome(int status, String code) {
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

    private String createFutureGame(AuthSession owner, int maxPlayers) throws Exception {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        CreateGameRequest request = new CreateGameRequest(
                "Roster Turf",
                "MG Road",
                new BigDecimal("12.9716"),
                new BigDecimal("77.5946"),
                LocalDate.ofInstant(start, INDIA),
                start,
                start.plus(2, ChronoUnit.HOURS),
                GameFormat.FIVE_V5,
                SkillLevel.BEGINNER,
                maxPlayers,
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

    private UUID insertGame(UUID ownerId, GameStatus status, int maxPlayers) {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        Game game = new Game();
        game.setOwnerId(ownerId);
        game.setTurfName("Direct Turf");
        game.setTurfAddress("Addr");
        game.setLatitude(new BigDecimal("12.9716"));
        game.setLongitude(new BigDecimal("77.5946"));
        game.setGameDate(LocalDate.ofInstant(start, INDIA));
        game.setStartTime(start);
        game.setEndTime(end);
        game.setFormat(GameFormat.FIVE_V5);
        game.setSkillLevel(SkillLevel.BEGINNER);
        game.setMaximumPlayers(maxPlayers);
        game.setRequiredPlayers(null);
        game.setJoiningFee(0);
        game.setDescription(null);
        game.setStatus(status);
        return gameRepository.save(game).getId();
    }

    private void insertParticipant(UUID gameId, UUID userId, MatchParticipant.Role role) {
        MatchParticipant participant = new MatchParticipant();
        participant.setGameId(gameId);
        participant.setUserId(userId);
        participant.setRole(role);
        participant.setAttended(null);
        matchParticipantRepository.save(participant);
    }

    private void insertJoinRequest(UUID gameId, UUID userId, RequestStatus status) {
        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setGameId(gameId);
        joinRequest.setUserId(userId);
        joinRequest.setStatus(status);
        joinRequestRepository.save(joinRequest);
    }

    private MvcResult createRequest(AuthSession player, UUID gameId) throws Exception {
        return mockMvc.perform(post("/api/games/{gameId}/join-requests", gameId)
                        .cookie(authCookie(player.token)))
                .andReturn();
    }

    private String requestIdFor(UUID gameId, UUID userId) {
        return joinRequestRepository.findByGameIdAndUserId(gameId, userId)
                .orElseThrow()
                .getId()
                .toString();
    }

    private long countRequests(UUID gameId, RequestStatus status) {
        return joinRequestRepository.findByGameIdOrderByCreatedAtAsc(gameId).stream()
                .filter(r -> r.getStatus() == status)
                .count();
    }

    private DecideOutcome decide(AuthSession owner, UUID gameId, String requestId, String action) {
        try {
            MvcResult result = mockMvc.perform(
                            patch("/api/games/{gameId}/join-requests/{requestId}", gameId, requestId)
                                    .cookie(authCookie(owner.token))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"action\":\"" + action + "\"}"))
                    .andReturn();
            int responseStatus = result.getResponse().getStatus();
            String code = null;
            if (responseStatus != 200) {
                code = objectMapper.readTree(result.getResponse().getContentAsString())
                        .get("code").asText();
            }
            return new DecideOutcome(responseStatus, code);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    // ── create: happy path ───────────────────────────────────────────────

    @Test
    void createJoinRequest_success_returns201Pending() throws Exception {
        AuthSession owner = registerAndAuth("jr-owner@example.com", "JR Owner");
        AuthSession player = registerAndAuth("jr-player@example.com", "JR Player");
        UUID gameId = UUID.fromString(createFutureGame(owner, 10));

        mockMvc.perform(post("/api/games/{gameId}/join-requests", gameId)
                        .cookie(authCookie(player.token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.gameId").value(gameId.toString()))
                .andExpect(jsonPath("$.userId").value(player.userId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        assertThat(matchParticipantRepository.countByGameId(gameId)).isEqualTo(1);
    }

    @Test
    void createJoinRequest_unknownGame_returns404() throws Exception {
        AuthSession player = registerAndAuth("jr-unknown@example.com", "JR Unknown");

        mockMvc.perform(post("/api/games/{gameId}/join-requests", UUID.randomUUID())
                        .cookie(authCookie(player.token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_NOT_FOUND"));
    }

    // ── create: conflict rules ───────────────────────────────────────────

    @Test
    void createJoinRequest_ownGame_returns409CannotJoinOwnGame() throws Exception {
        AuthSession owner = registerAndAuth("jr-own@example.com", "JR Own");
        UUID gameId = UUID.fromString(createFutureGame(owner, 10));

        mockMvc.perform(post("/api/games/{gameId}/join-requests", gameId)
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CANNOT_JOIN_OWN_GAME"));
    }

    @Test
    void createJoinRequest_nonOpenStatuses_returns409GameNotJoinable() throws Exception {
        AuthSession owner = registerAndAuth("jr-nonopen@example.com", "JR NonOpen");
        AuthSession player = registerAndAuth("jr-nonopen-p@example.com", "JR NonOpen P");

        for (GameStatus status : List.of(GameStatus.FULL, GameStatus.IN_PROGRESS,
                GameStatus.COMPLETED, GameStatus.CANCELLED)) {
            UUID gameId = insertGame(owner.userId, status, 10);
            mockMvc.perform(post("/api/games/{gameId}/join-requests", gameId)
                            .cookie(authCookie(player.token)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("GAME_NOT_JOINABLE"));
        }
    }

    @Test
    void createJoinRequest_alreadyParticipant_returns409AlreadyParticipant() throws Exception {
        AuthSession owner = registerAndAuth("jr-part@example.com", "JR Part");
        AuthSession player = registerAndAuth("jr-part-p@example.com", "JR Part P");
        UUID gameId = UUID.fromString(createFutureGame(owner, 10));
        insertParticipant(gameId, player.userId, MatchParticipant.Role.PLAYER);

        mockMvc.perform(post("/api/games/{gameId}/join-requests", gameId)
                        .cookie(authCookie(player.token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_PARTICIPANT"));
    }

    @Test
    void createJoinRequest_openButFull_returns409GameFull() throws Exception {
        AuthSession owner = registerAndAuth("jr-full@example.com", "JR Full");
        AuthSession player = registerAndAuth("jr-full-p@example.com", "JR Full P");
        UUID gameId = insertGame(owner.userId, GameStatus.OPEN, 5);
        insertParticipant(gameId, owner.userId, MatchParticipant.Role.OWNER);
        for (int i = 0; i < 4; i++) {
            insertParticipant(gameId, registerAndAuth("jr-full-f" + i + "@example.com", "F" + i).userId,
                    MatchParticipant.Role.PLAYER);
        }

        mockMvc.perform(post("/api/games/{gameId}/join-requests", gameId)
                        .cookie(authCookie(player.token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_FULL"));
    }

    @Test
    void createJoinRequest_duplicatePending_returns409JoinRequestExists() throws Exception {
        AuthSession owner = registerAndAuth("jr-dup@example.com", "JR Dup");
        AuthSession player = registerAndAuth("jr-dup-p@example.com", "JR Dup P");
        UUID gameId = UUID.fromString(createFutureGame(owner, 10));

        createRequest(player, gameId);
        mockMvc.perform(post("/api/games/{gameId}/join-requests", gameId)
                        .cookie(authCookie(player.token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOIN_REQUEST_EXISTS"));
    }

    // ── GET /me ──────────────────────────────────────────────────────────

    @Test
    void getMyRequest_withoutRequest_returns404RequestNotFound() throws Exception {
        AuthSession owner = registerAndAuth("jr-me-owner@example.com", "JR Me Owner");
        AuthSession player = registerAndAuth("jr-me@example.com", "JR Me");
        UUID gameId = UUID.fromString(createFutureGame(owner, 10));

        mockMvc.perform(get("/api/games/{gameId}/join-requests/me", gameId)
                        .cookie(authCookie(player.token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REQUEST_NOT_FOUND"));
    }

    @Test
    void getMyRequest_afterCreate_returns200Pending() throws Exception {
        AuthSession owner = registerAndAuth("jr-me2-owner@example.com", "JR Me2 Owner");
        AuthSession player = registerAndAuth("jr-me2@example.com", "JR Me2");
        UUID gameId = UUID.fromString(createFutureGame(owner, 10));
        createRequest(player, gameId);

        mockMvc.perform(get("/api/games/{gameId}/join-requests/me", gameId)
                        .cookie(authCookie(player.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestIdFor(gameId, player.userId)))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // ── re-request rules ─────────────────────────────────────────────────

    @Test
    void reRequest_afterReject_resetsExistingRowToPending() throws Exception {
        AuthSession owner = registerAndAuth("jr-re@example.com", "JR Re");
        AuthSession player = registerAndAuth("jr-re-p@example.com", "JR Re P");
        UUID gameId = UUID.fromString(createFutureGame(owner, 10));

        createRequest(player, gameId);
        String originalId = requestIdFor(gameId, player.userId);
        decide(owner, gameId, originalId, "REJECT");
        assertThat(joinRequestRepository.findById(UUID.fromString(originalId)).orElseThrow().getStatus())
                .isEqualTo(RequestStatus.REJECTED);

        mockMvc.perform(post("/api/games/{gameId}/join-requests", gameId)
                        .cookie(authCookie(player.token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(originalId))
                .andExpect(jsonPath("$.status").value("PENDING"));

        JoinRequest reset = joinRequestRepository.findById(UUID.fromString(originalId)).orElseThrow();
        assertThat(reset.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(reset.getDecidedAt()).isNull();
        assertThat(joinRequestRepository.findByGameIdOrderByCreatedAtAsc(gameId)).hasSize(1);
    }

    @Test
    void reRequest_afterAccept_returns409AlreadyParticipant() throws Exception {
        AuthSession owner = registerAndAuth("jr-acc@example.com", "JR Acc");
        AuthSession player = registerAndAuth("jr-acc-p@example.com", "JR Acc P");
        UUID gameId = UUID.fromString(createFutureGame(owner, 10));

        createRequest(player, gameId);
        DecideOutcome accept = decide(owner, gameId, requestIdFor(gameId, player.userId), "ACCEPT");
        assertThat(accept.status()).isEqualTo(200);

        mockMvc.perform(post("/api/games/{gameId}/join-requests", gameId)
                        .cookie(authCookie(player.token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_PARTICIPANT"));
    }

    @Test
    void reRequest_whenFull_returns409GameFull() throws Exception {
        AuthSession owner = registerAndAuth("jr-ref@example.com", "JR Ref");
        AuthSession player = registerAndAuth("jr-ref-p@example.com", "JR Ref P");
        UUID gameId = insertGame(owner.userId, GameStatus.OPEN, 5);
        insertParticipant(gameId, owner.userId, MatchParticipant.Role.OWNER);
        for (int i = 0; i < 4; i++) {
            insertParticipant(gameId, registerAndAuth("jr-ref-f" + i + "@example.com", "RF" + i).userId,
                    MatchParticipant.Role.PLAYER);
        }
        insertJoinRequest(gameId, player.userId, RequestStatus.REJECTED);

        mockMvc.perform(post("/api/games/{gameId}/join-requests", gameId)
                        .cookie(authCookie(player.token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_FULL"));
    }

    // ── decide: success + conflicts ──────────────────────────────────────

    @Test
    void decide_accept_success_addsPlayerAndReturnsAccepted() throws Exception {
        AuthSession owner = registerAndAuth("jr-da@example.com", "JR Da");
        AuthSession player = registerAndAuth("jr-da-p@example.com", "JR Da P");
        UUID gameId = UUID.fromString(createFutureGame(owner, 10));
        createRequest(player, gameId);

        mockMvc.perform(patch("/api/games/{gameId}/join-requests/{requestId}",
                        gameId, requestIdFor(gameId, player.userId))
                        .cookie(authCookie(owner.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"ACCEPT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.decidedAt").exists());

        assertThat(matchParticipantRepository.existsByGameIdAndUserId(gameId, player.userId)).isTrue();
        assertThat(matchParticipantRepository.countByGameId(gameId)).isEqualTo(2);
    }

    @Test
    void decide_reject_success() throws Exception {
        AuthSession owner = registerAndAuth("jr-dr@example.com", "JR Dr");
        AuthSession player = registerAndAuth("jr-dr-p@example.com", "JR Dr P");
        UUID gameId = UUID.fromString(createFutureGame(owner, 10));
        createRequest(player, gameId);

        mockMvc.perform(patch("/api/games/{gameId}/join-requests/{requestId}",
                        gameId, requestIdFor(gameId, player.userId))
                        .cookie(authCookie(owner.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"REJECT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.decidedAt").exists());

        assertThat(matchParticipantRepository.existsByGameIdAndUserId(gameId, player.userId)).isFalse();
    }

    @Test
    void decide_alreadyDecided_returns409RequestAlreadyDecided() throws Exception {
        AuthSession owner = registerAndAuth("jr-ad@example.com", "JR Ad");
        AuthSession player = registerAndAuth("jr-ad-p@example.com", "JR Ad P");
        UUID gameId = UUID.fromString(createFutureGame(owner, 10));
        createRequest(player, gameId);
        String requestId = requestIdFor(gameId, player.userId);
        assertThat(decide(owner, gameId, requestId, "REJECT").status()).isEqualTo(200);

        DecideOutcome second = decide(owner, gameId, requestId, "ACCEPT");
        assertThat(second.status()).isEqualTo(409);
        assertThat(second.code()).isEqualTo("REQUEST_ALREADY_DECIDED");
    }

    @Test
    void decide_nonOwner_returns403Forbidden() throws Exception {
        AuthSession owner = registerAndAuth("jr-no@example.com", "JR No");
        AuthSession stranger = registerAndAuth("jr-no-s@example.com", "JR No S");
        AuthSession player = registerAndAuth("jr-no-p@example.com", "JR No P");
        UUID gameId = UUID.fromString(createFutureGame(owner, 10));
        createRequest(player, gameId);

        DecideOutcome outcome = decide(stranger, gameId, requestIdFor(gameId, player.userId), "ACCEPT");
        assertThat(outcome.status()).isEqualTo(403);
        assertThat(outcome.code()).isEqualTo("FORBIDDEN");
    }

    @Test
    void decide_unknownRequest_returns404RequestNotFound() throws Exception {
        AuthSession owner = registerAndAuth("jr-ur@example.com", "JR Ur");
        UUID gameId = UUID.fromString(createFutureGame(owner, 10));

        DecideOutcome outcome = decide(owner, gameId, UUID.randomUUID().toString(), "ACCEPT");
        assertThat(outcome.status()).isEqualTo(404);
        assertThat(outcome.code()).isEqualTo("REQUEST_NOT_FOUND");
    }

    @Test
    void decide_requestFromAnotherGame_returns404RequestNotFound() throws Exception {
        AuthSession owner = registerAndAuth("jr-og@example.com", "JR Og");
        AuthSession player = registerAndAuth("jr-og-p@example.com", "JR Og P");
        UUID gameOne = UUID.fromString(createFutureGame(owner, 10));
        UUID gameTwo = UUID.fromString(createFutureGame(owner, 10));
        createRequest(player, gameTwo);
        String requestOnGameTwo = requestIdFor(gameTwo, player.userId);

        DecideOutcome outcome = decide(owner, gameOne, requestOnGameTwo, "ACCEPT");
        assertThat(outcome.status()).isEqualTo(404);
        assertThat(outcome.code()).isEqualTo("REQUEST_NOT_FOUND");
    }

    @Test
    void listRequests_nonOwner_returns403Forbidden() throws Exception {
        AuthSession owner = registerAndAuth("jr-list@example.com", "JR List");
        AuthSession stranger = registerAndAuth("jr-list-s@example.com", "JR List S");
        UUID gameId = UUID.fromString(createFutureGame(owner, 10));

        mockMvc.perform(get("/api/games/{gameId}/join-requests", gameId)
                        .cookie(authCookie(stranger.token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // ── accept: last slot flips game to FULL ─────────────────────────────

    @Test
    void decide_acceptLastSlot_setsGameFull() throws Exception {
        AuthSession owner = registerAndAuth("jr-last@example.com", "JR Last");
        AuthSession player = registerAndAuth("jr-last-p@example.com", "JR Last P");
        UUID gameId = UUID.fromString(createFutureGame(owner, 5));
        for (int i = 0; i < 3; i++) {
            insertParticipant(gameId, registerAndAuth("jr-last-f" + i + "@example.com", "LF" + i).userId,
                    MatchParticipant.Role.PLAYER);
        }
        createRequest(player, gameId);

        DecideOutcome outcome = decide(owner, gameId, requestIdFor(gameId, player.userId), "ACCEPT");
        assertThat(outcome.status()).isEqualTo(200);

        assertThat(matchParticipantRepository.countByGameId(gameId)).isEqualTo(5);
        assertThat(gameRepository.findById(gameId).orElseThrow().getStatus()).isEqualTo(GameStatus.FULL);
    }

    // ── concurrency: one winner for the final slot ───────────────────────

    @Test
    void concurrent_acceptForFinalSlot_admitsExactlyOneAndNeverExceedsCapacity() throws Exception {
        AuthSession owner = registerAndAuth("jr-cc-owner@example.com", "JR CC Owner");
        UUID gameId = UUID.fromString(createFutureGame(owner, 5));
        for (int i = 0; i < 3; i++) {
            insertParticipant(gameId, registerAndAuth("jr-cc-f" + i + "@example.com", "CF" + i).userId,
                    MatchParticipant.Role.PLAYER);
        }
        AuthSession first = registerAndAuth("jr-cc-1@example.com", "JR CC 1");
        AuthSession second = registerAndAuth("jr-cc-2@example.com", "JR CC 2");
        createRequest(first, gameId);
        createRequest(second, gameId);
        String firstRequest = requestIdFor(gameId, first.userId);
        String secondRequest = requestIdFor(gameId, second.userId);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            Callable<DecideOutcome> acceptOne = () -> {
                ready.countDown();
                go.await();
                return decide(owner, gameId, firstRequest, "ACCEPT");
            };
            Callable<DecideOutcome> acceptTwo = () -> {
                ready.countDown();
                go.await();
                return decide(owner, gameId, secondRequest, "ACCEPT");
            };
            Future<DecideOutcome> one = pool.submit(acceptOne);
            Future<DecideOutcome> two = pool.submit(acceptTwo);
            assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
            go.countDown();

            DecideOutcome outOne = one.get(60, TimeUnit.SECONDS);
            DecideOutcome outTwo = two.get(60, TimeUnit.SECONDS);

            assertThat(List.of(outOne.status(), outTwo.status()))
                    .containsExactlyInAnyOrder(200, 409);
            DecideOutcome loser = outOne.status() == 409 ? outOne : outTwo;
            assertThat(loser.code()).isIn("GAME_FULL", "GAME_NOT_JOINABLE");

            assertThat(matchParticipantRepository.countByGameId(gameId)).isEqualTo(5);
            assertThat(gameRepository.findById(gameId).orElseThrow().getStatus())
                    .isEqualTo(GameStatus.FULL);
            assertThat(countRequests(gameId, RequestStatus.ACCEPTED)).isEqualTo(1);
            assertThat(countRequests(gameId, RequestStatus.PENDING)).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
