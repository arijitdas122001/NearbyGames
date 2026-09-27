package com.gameconnect.game;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.gameconnect.game.entity.JoinRequest;
import com.gameconnect.game.entity.JoinRequest.RequestStatus;
import com.gameconnect.game.entity.MatchParticipant;
import com.gameconnect.game.repository.GameRepository;
import com.gameconnect.game.repository.JoinRequestRepository;
import com.gameconnect.game.repository.MatchParticipantRepository;
import com.gameconnect.security.JwtService;

import tools.jackson.databind.ObjectMapper;

/**
 * Phase 9 "My Games": games the authenticated user is involved in, across the
 * whole lifecycle. The headline guarantee is that a game never disappears from
 * this list once its start time has passed.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class MyGamesIntegrationTest {

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

    private UUID insertGame(UUID ownerId, String turfName, Instant start, Instant end,
                            GameStatus status) {
        Game game = new Game();
        game.setOwnerId(ownerId);
        game.setTurfName(turfName);
        game.setTurfAddress("Addr " + turfName);
        game.setLatitude(new BigDecimal("12.9716"));
        game.setLongitude(new BigDecimal("77.5946"));
        game.setGameDate(LocalDate.ofInstant(start, INDIA));
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

    private void insertJoinRequest(UUID gameId, UUID userId, RequestStatus status) {
        JoinRequest request = new JoinRequest();
        request.setGameId(gameId);
        request.setUserId(userId);
        request.setStatus(status);
        joinRequestRepository.save(request);
    }

    private Instant futureAt(long hoursFromNow) {
        return Instant.now().plus(hoursFromNow, ChronoUnit.HOURS);
    }

    // ── authentication ──────────────────────────────────────────────────

    @Test
    void myGames_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/games/my"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Guards against Spring binding "my" to the /{id} UUID path variable, which
     * would surface as a 400 VALIDATION_ERROR instead of a paged response.
     */
    @Test
    void myGames_pathIsNotCapturedByGameIdVariable() throws Exception {
        AuthSession user = registerAndAuth("routing@example.com", "Routing User");

        mockMvc.perform(get("/api/games/my").cookie(authCookie(user.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    // ── ownership / participation ───────────────────────────────────────

    @Test
    void myGames_owner_seesOwnedGame_withOwnerRole() throws Exception {
        AuthSession owner = registerAndAuth("owner@example.com", "Owner");
        UUID gameId = insertGame(owner.userId(), "Owned Turf",
                futureAt(24), futureAt(26), GameStatus.OPEN);
        insertParticipant(gameId, owner.userId(), MatchParticipant.Role.OWNER, null);

        mockMvc.perform(get("/api/games/my").cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(gameId.toString()))
                .andExpect(jsonPath("$.content[0].turfName").value("Owned Turf"))
                .andExpect(jsonPath("$.content[0].myRole").value("OWNER"))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[0].category").value("UPCOMING"));
    }

    @Test
    void myGames_acceptedParticipant_seesGame_withPlayerRole() throws Exception {
        AuthSession owner = registerAndAuth("p-owner@example.com", "P Owner");
        AuthSession player = registerAndAuth("p-player@example.com", "P Player");
        UUID gameId = insertGame(owner.userId(), "Shared Turf",
                futureAt(24), futureAt(26), GameStatus.OPEN);
        insertParticipant(gameId, owner.userId(), MatchParticipant.Role.OWNER, null);
        insertParticipant(gameId, player.userId(), MatchParticipant.Role.PLAYER, null);

        mockMvc.perform(get("/api/games/my").cookie(authCookie(player.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(gameId.toString()))
                .andExpect(jsonPath("$.content[0].myRole").value("PLAYER"));
    }

    @Test
    void myGames_participant_seesOwnAttendanceFlag() throws Exception {
        AuthSession owner = registerAndAuth("att-owner@example.com", "Att Owner");
        AuthSession attended = registerAndAuth("att-yes@example.com", "Attended");
        AuthSession absent = registerAndAuth("att-no@example.com", "Absent");
        Instant start = Instant.now().minus(9, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(7, ChronoUnit.HOURS);
        UUID gameId = insertGame(owner.userId(), "Done Turf", start, end, GameStatus.COMPLETED);
        insertParticipant(gameId, attended.userId(), MatchParticipant.Role.PLAYER, true);
        insertParticipant(gameId, absent.userId(), MatchParticipant.Role.PLAYER, false);

        mockMvc.perform(get("/api/games/my").cookie(authCookie(attended.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].myAttended").value(true))
                .andExpect(jsonPath("$.content[0].canRate").value(true));

        mockMvc.perform(get("/api/games/my").cookie(authCookie(absent.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].myAttended").value(false))
                .andExpect(jsonPath("$.content[0].canRate").value(false));
    }

    // ── pending / rejected / cancelled requests are excluded ────────────

    @Test
    void myGames_pendingRequester_doesNotSeeGame() throws Exception {
        AuthSession owner = registerAndAuth("jr1-owner@example.com", "JR1 Owner");
        AuthSession pending = registerAndAuth("jr1-pending@example.com", "JR1 Pending");
        UUID gameId = insertGame(owner.userId(), "Pending Turf",
                futureAt(24), futureAt(26), GameStatus.OPEN);
        insertJoinRequest(gameId, pending.userId(), RequestStatus.PENDING);

        mockMvc.perform(get("/api/games/my").cookie(authCookie(pending.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void myGames_rejectedRequester_doesNotSeeGame() throws Exception {
        AuthSession owner = registerAndAuth("jr2-owner@example.com", "JR2 Owner");
        AuthSession rejected = registerAndAuth("jr2-rejected@example.com", "JR2 Rejected");
        UUID gameId = insertGame(owner.userId(), "Rejected Turf",
                futureAt(24), futureAt(26), GameStatus.OPEN);
        insertJoinRequest(gameId, rejected.userId(), RequestStatus.REJECTED);

        mockMvc.perform(get("/api/games/my").cookie(authCookie(rejected.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void myGames_cancelledRequester_doesNotSeeGame() throws Exception {
        AuthSession owner = registerAndAuth("jr3-owner@example.com", "JR3 Owner");
        AuthSession cancelled = registerAndAuth("jr3-cancelled@example.com", "JR3 Cancelled");
        UUID gameId = insertGame(owner.userId(), "Cancelled Request Turf",
                futureAt(24), futureAt(26), GameStatus.OPEN);
        insertJoinRequest(gameId, cancelled.userId(), RequestStatus.CANCELLED);

        mockMvc.perform(get("/api/games/my").cookie(authCookie(cancelled.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ── isolation ───────────────────────────────────────────────────────

    @Test
    void myGames_unrelatedUser_doesNotSeeOtherUsersGame() throws Exception {
        AuthSession owner = registerAndAuth("iso-owner@example.com", "Iso Owner");
        AuthSession stranger = registerAndAuth("iso-stranger@example.com", "Iso Stranger");
        UUID gameId = insertGame(owner.userId(), "Private Turf",
                futureAt(24), futureAt(26), GameStatus.OPEN);
        insertParticipant(gameId, owner.userId(), MatchParticipant.Role.OWNER, null);

        mockMvc.perform(get("/api/games/my").cookie(authCookie(stranger.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void myGames_ignoresUserIdParameter() throws Exception {
        AuthSession owner = registerAndAuth("uid-owner@example.com", "Uid Owner");
        AuthSession attacker = registerAndAuth("uid-attacker@example.com", "Uid Attacker");
        UUID gameId = insertGame(owner.userId(), "Uid Turf",
                futureAt(24), futureAt(26), GameStatus.OPEN);
        insertParticipant(gameId, owner.userId(), MatchParticipant.Role.OWNER, null);

        mockMvc.perform(get("/api/games/my")
                        .param("userId", owner.userId().toString())
                        .cookie(authCookie(attacker.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ── lifecycle coverage ──────────────────────────────────────────────

    @Test
    void myGames_lifecycleStatuses_landInTheirOwnCategory() throws Exception {
        AuthSession owner = registerAndAuth("lc-owner@example.com", "LC Owner");
        UUID open = insertGame(owner.userId(), "Open Turf",
                futureAt(24), futureAt(26), GameStatus.OPEN);
        UUID full = insertGame(owner.userId(), "Full Turf",
                futureAt(28), futureAt(30), GameStatus.FULL);
        UUID inProgress = insertGame(owner.userId(), "Live Turf",
                futureAt(1), futureAt(3), GameStatus.IN_PROGRESS);
        Instant pastStart = Instant.now().minus(9, ChronoUnit.HOURS);
        Instant pastEnd = Instant.now().minus(7, ChronoUnit.HOURS);
        UUID completed = insertGame(owner.userId(), "Done Turf",
                pastStart, pastEnd, GameStatus.COMPLETED);
        UUID cancelled = insertGame(owner.userId(), "Dead Turf",
                futureAt(40), futureAt(42), GameStatus.CANCELLED);
        for (UUID gameId : java.util.List.of(open, full, inProgress, completed, cancelled)) {
            insertParticipant(gameId, owner.userId(), MatchParticipant.Role.OWNER, null);
        }

        mockMvc.perform(get("/api/games/my")
                        .param("category", "UPCOMING")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.id == '" + open + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id == '" + full + "')]").exists());

        mockMvc.perform(get("/api/games/my")
                        .param("category", "IN_PROGRESS")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(inProgress.toString()));

        mockMvc.perform(get("/api/games/my")
                        .param("category", "COMPLETED")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(completed.toString()))
                .andExpect(jsonPath("$.content[0].category").value("COMPLETED"));

        mockMvc.perform(get("/api/games/my")
                        .param("category", "CANCELLED")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(cancelled.toString()))
                .andExpect(jsonPath("$.content[0].category").value("CANCELLED"));
    }

    @Test
    void myGames_defaultCategoryAll_returnsEveryRelevantGame() throws Exception {
        AuthSession owner = registerAndAuth("all-owner@example.com", "All Owner");
        UUID open = insertGame(owner.userId(), "All Open",
                futureAt(24), futureAt(26), GameStatus.OPEN);
        UUID completed = insertGame(owner.userId(), "All Done",
                Instant.now().minus(9, ChronoUnit.HOURS),
                Instant.now().minus(7, ChronoUnit.HOURS),
                GameStatus.COMPLETED);
        insertParticipant(open, owner.userId(), MatchParticipant.Role.OWNER, null);
        insertParticipant(completed, owner.userId(), MatchParticipant.Role.OWNER, true);

        mockMvc.perform(get("/api/games/my").cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    // ── the regression this phase exists for ────────────────────────────

    @Test
    void myGames_completedGameWithPastStartTime_isStillReturned() throws Exception {
        AuthSession owner = registerAndAuth("reg-owner@example.com", "Reg Owner");
        UUID gameId = insertGame(owner.userId(), "Yesterday Turf",
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now().minus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS),
                GameStatus.COMPLETED);
        insertParticipant(gameId, owner.userId(), MatchParticipant.Role.OWNER, true);

        // My Games still returns it...
        mockMvc.perform(get("/api/games/my")
                        .param("category", "COMPLETED")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(gameId.toString()));

        // ...while discovery has dropped it, even for a participant.
        mockMvc.perform(get("/api/games").cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void myGames_gameInAttendanceWindow_pastStartTime_stillVisible() throws Exception {
        AuthSession owner = registerAndAuth("win-owner@example.com", "Win Owner");
        UUID gameId = insertGame(owner.userId(), "Underway Turf",
                Instant.now().minus(1, ChronoUnit.HOURS),
                Instant.now().plus(1, ChronoUnit.HOURS),
                GameStatus.OPEN);
        insertParticipant(gameId, owner.userId(), MatchParticipant.Role.OWNER, null);

        mockMvc.perform(get("/api/games/my")
                        .param("category", "UPCOMING")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(gameId.toString()));

        mockMvc.perform(get("/api/games").cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ── deduplication ───────────────────────────────────────────────────

    @Test
    void myGames_ownerWithOwnerParticipantRow_returnsGameOnce() throws Exception {
        AuthSession owner = registerAndAuth("dedup-owner@example.com", "Dedup Owner");
        UUID gameId = insertGame(owner.userId(), "Dedup Turf",
                futureAt(24), futureAt(26), GameStatus.OPEN);
        insertParticipant(gameId, owner.userId(), MatchParticipant.Role.OWNER, null);

        mockMvc.perform(get("/api/games/my").cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void myGames_ownerWithoutParticipantRow_stillSeesGame_asOwner() throws Exception {
        AuthSession owner = registerAndAuth("nop-row@example.com", "No Row");
        UUID gameId = insertGame(owner.userId(), "Rowless Turf",
                futureAt(24), futureAt(26), GameStatus.OPEN);

        mockMvc.perform(get("/api/games/my").cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(gameId.toString()))
                .andExpect(jsonPath("$.content[0].myRole").value("OWNER"))
                .andExpect(jsonPath("$.content[0].myAttended").doesNotExist());
    }

    // ── validation ──────────────────────────────────────────────────────

    @Test
    void myGames_unknownCategory_returns400() throws Exception {
        AuthSession user = registerAndAuth("bad-cat@example.com", "Bad Cat");
        mockMvc.perform(get("/api/games/my")
                        .param("category", "NOPE")
                        .cookie(authCookie(user.token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void myGames_invalidPagination_returns400() throws Exception {
        AuthSession user = registerAndAuth("bad-page@example.com", "Bad Page");
        mockMvc.perform(get("/api/games/my").param("page", "-1")
                        .cookie(authCookie(user.token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/games/my").param("size", "0")
                        .cookie(authCookie(user.token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/games/my").param("size", "101")
                        .cookie(authCookie(user.token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/games/my").param("page", "abc")
                        .cookie(authCookie(user.token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── pagination / ordering ───────────────────────────────────────────

    @Test
    void myGames_pagination_returnsExpectedPages() throws Exception {
        AuthSession owner = registerAndAuth("pg-owner@example.com", "Pg Owner");
        for (int i = 0; i < 5; i++) {
            UUID gameId = insertGame(owner.userId(), "Pg Turf " + i,
                    futureAt(24 + i * 2L), futureAt(25 + i * 2L), GameStatus.OPEN);
            insertParticipant(gameId, owner.userId(), MatchParticipant.Role.OWNER, null);
        }

        mockMvc.perform(get("/api/games/my")
                        .param("page", "0").param("size", "2")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/games/my")
                        .param("page", "2").param("size", "2")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true));

        mockMvc.perform(get("/api/games/my")
                        .param("page", "99").param("size", "20")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void myGames_upcoming_orderedByStartTimeAscending() throws Exception {
        AuthSession owner = registerAndAuth("sort-up@example.com", "Sort Up");
        UUID far = insertGame(owner.userId(), "Far Turf",
                futureAt(72), futureAt(74), GameStatus.OPEN);
        UUID near = insertGame(owner.userId(), "Near Turf",
                futureAt(2), futureAt(4), GameStatus.OPEN);
        insertParticipant(far, owner.userId(), MatchParticipant.Role.OWNER, null);
        insertParticipant(near, owner.userId(), MatchParticipant.Role.OWNER, null);

        mockMvc.perform(get("/api/games/my")
                        .param("category", "UPCOMING")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(near.toString()))
                .andExpect(jsonPath("$.content[1].id").value(far.toString()));
    }

    @Test
    void myGames_completed_orderedByStartTimeDescending() throws Exception {
        AuthSession owner = registerAndAuth("sort-co@example.com", "Sort Co");
        UUID older = insertGame(owner.userId(), "Older Turf",
                Instant.now().minus(10, ChronoUnit.DAYS),
                Instant.now().minus(10, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS),
                GameStatus.COMPLETED);
        UUID newer = insertGame(owner.userId(), "Newer Turf",
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now().minus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS),
                GameStatus.COMPLETED);
        insertParticipant(older, owner.userId(), MatchParticipant.Role.OWNER, null);
        insertParticipant(newer, owner.userId(), MatchParticipant.Role.OWNER, null);

        mockMvc.perform(get("/api/games/my")
                        .param("category", "COMPLETED")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(newer.toString()))
                .andExpect(jsonPath("$.content[1].id").value(older.toString()));
    }

    @Test
    void myGames_ordering_isStableAcrossRepeatedCalls() throws Exception {
        AuthSession owner = registerAndAuth("stable@example.com", "Stable");
        // Identical start times, so only the id tiebreaker can guarantee order.
        Instant shared = futureAt(30);
        for (int i = 0; i < 4; i++) {
            UUID gameId = insertGame(owner.userId(), "Tie Turf " + i,
                    shared, shared.plus(2, ChronoUnit.HOURS), GameStatus.COMPLETED);
            insertParticipant(gameId, owner.userId(), MatchParticipant.Role.OWNER, null);
        }

        String first = idsOfMyGames(owner.token, "COMPLETED");
        String second = idsOfMyGames(owner.token, "COMPLETED");
        String third = idsOfMyGames(owner.token, "COMPLETED");

        org.assertj.core.api.Assertions.assertThat(first)
                .isEqualTo(second)
                .isEqualTo(third);
    }

    private String idsOfMyGames(String token, String category) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/games/my")
                        .param("category", category)
                        .cookie(authCookie(token)))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    // ── Phase 8 rating integration ──────────────────────────────────────

    @Test
    void myGames_completedGameFromMyGames_isRateableAndSubmittable() throws Exception {
        AuthSession owner = registerAndAuth("rt-owner@example.com", "Rt Owner");
        AuthSession player = registerAndAuth("rt-player@example.com", "Rt Player");
        UUID gameId = insertGame(owner.userId(), "Rateable Turf",
                Instant.now().minus(9, ChronoUnit.HOURS),
                Instant.now().minus(7, ChronoUnit.HOURS),
                GameStatus.COMPLETED);
        insertParticipant(gameId, owner.userId(), MatchParticipant.Role.OWNER, true);
        insertParticipant(gameId, player.userId(), MatchParticipant.Role.PLAYER, true);

        MvcResult mine = mockMvc.perform(get("/api/games/my")
                        .param("category", "COMPLETED")
                        .cookie(authCookie(player.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].canRate").value(true))
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(
                        mine.getResponse().getContentAsString())
                .contains(gameId.toString());

        mockMvc.perform(get("/api/games/{gameId}/ratings/eligible", gameId)
                        .cookie(authCookie(player.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(gameId.toString()))
                .andExpect(jsonPath("$.players.length()").value(1))
                .andExpect(jsonPath("$.players[0].userId").value(owner.userId().toString()));

        mockMvc.perform(post("/api/games/{gameId}/ratings", gameId)
                        .cookie(authCookie(player.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ratedPlayerId\":\"" + owner.userId()
                                + "\",\"score\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(5));
    }

    @Test
    void myGames_absentParticipant_cannotSubmitRating() throws Exception {
        AuthSession owner = registerAndAuth("ra-owner@example.com", "Ra Owner");
        AuthSession absent = registerAndAuth("ra-absent@example.com", "Ra Absent");
        UUID gameId = insertGame(owner.userId(), "Absent Turf",
                Instant.now().minus(9, ChronoUnit.HOURS),
                Instant.now().minus(7, ChronoUnit.HOURS),
                GameStatus.COMPLETED);
        insertParticipant(gameId, owner.userId(), MatchParticipant.Role.OWNER, true);
        insertParticipant(gameId, absent.userId(), MatchParticipant.Role.PLAYER, false);

        mockMvc.perform(get("/api/games/my")
                        .param("category", "COMPLETED")
                        .cookie(authCookie(absent.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].canRate").value(false));

        mockMvc.perform(post("/api/games/{gameId}/ratings", gameId)
                        .cookie(authCookie(absent.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ratedPlayerId\":\"" + owner.userId()
                                + "\",\"score\":4}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RATER_ATTENDANCE_REQUIRED"));
    }

    @Test
    void myGames_nonParticipant_cannotSubmitRating() throws Exception {
        AuthSession owner = registerAndAuth("rn-owner@example.com", "Rn Owner");
        AuthSession stranger = registerAndAuth("rn-stranger@example.com", "Rn Stranger");
        UUID gameId = insertGame(owner.userId(), "Stranger Turf",
                Instant.now().minus(9, ChronoUnit.HOURS),
                Instant.now().minus(7, ChronoUnit.HOURS),
                GameStatus.COMPLETED);
        insertParticipant(gameId, owner.userId(), MatchParticipant.Role.OWNER, true);

        mockMvc.perform(post("/api/games/{gameId}/ratings", gameId)
                        .cookie(authCookie(stranger.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ratedPlayerId\":\"" + owner.userId()
                                + "\",\"score\":3}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RATER_NOT_PARTICIPANT"));
    }

    @Test
    void myGames_duplicateRating_returns409() throws Exception {
        AuthSession owner = registerAndAuth("rd-owner@example.com", "Rd Owner");
        AuthSession player = registerAndAuth("rd-player@example.com", "Rd Player");
        UUID gameId = insertGame(owner.userId(), "Dup Turf",
                Instant.now().minus(9, ChronoUnit.HOURS),
                Instant.now().minus(7, ChronoUnit.HOURS),
                GameStatus.COMPLETED);
        insertParticipant(gameId, owner.userId(), MatchParticipant.Role.OWNER, true);
        insertParticipant(gameId, player.userId(), MatchParticipant.Role.PLAYER, true);

        String body = "{\"ratedPlayerId\":\"" + owner.userId() + "\",\"score\":4}";
        mockMvc.perform(post("/api/games/{gameId}/ratings", gameId)
                        .cookie(authCookie(player.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/games/{gameId}/ratings", gameId)
                        .cookie(authCookie(player.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RATING_ALREADY_EXISTS"));
    }

    // ── discovery unchanged ─────────────────────────────────────────────

    @Test
    void myGames_discoveryStillExcludesCompletedGamesEvenForParticipants() throws Exception {
        AuthSession owner = registerAndAuth("disc-owner@example.com", "Disc Owner");
        UUID completed = insertGame(owner.userId(), "Hidden Turf",
                Instant.now().minus(9, ChronoUnit.HOURS),
                Instant.now().minus(7, ChronoUnit.HOURS),
                GameStatus.COMPLETED);
        UUID full = insertGame(owner.userId(), "Full Hidden",
                futureAt(24), futureAt(26), GameStatus.FULL);
        insertParticipant(completed, owner.userId(), MatchParticipant.Role.OWNER, true);
        insertParticipant(full, owner.userId(), MatchParticipant.Role.OWNER, null);

        mockMvc.perform(get("/api/games").cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/games/my").cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}
