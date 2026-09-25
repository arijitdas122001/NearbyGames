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
import com.gameconnect.game.scheduler.GameCompletionScheduler;
import com.gameconnect.game.service.AttendanceService;
import com.gameconnect.security.JwtService;

import tools.jackson.databind.ObjectMapper;

/**
 * Automatic game completion (Phase 7). The scheduled method is invoked
 * directly instead of waiting on real clock ticks; grace defaults to 6h so any
 * incidental scheduler fires only touch games with endTime far in the past.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class GameCompletionIntegrationTest {

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
    GameCompletionScheduler scheduler;

    @Autowired
    AttendanceService attendanceService;

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

    private GameStatus statusOf(UUID gameId) {
        return gameRepository.findById(gameId).orElseThrow().getStatus();
    }

    // ── scheduler completion ─────────────────────────────────────────────

    @Test
    void scheduler_completesOpenGameWhenPastEndTimePlusGrace() throws Exception {
        AuthSession owner = registerAndAuth("s-owner@example.com", "S Owner");
        Instant start = Instant.now().minus(9, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(7, ChronoUnit.HOURS);
        UUID gameId = insertGame(owner.userId, start, end, GameStatus.OPEN);
        insertParticipant(gameId, owner.userId, MatchParticipant.Role.OWNER, null);

        scheduler.completeDueGames();

        assertThat(statusOf(gameId)).isEqualTo(GameStatus.COMPLETED);
    }

    @Test
    void scheduler_completesFullAndInProgressGames() throws Exception {
        AuthSession owner = registerAndAuth("s2-owner@example.com", "S2 Owner");
        Instant end = Instant.now().minus(7, ChronoUnit.HOURS);
        UUID fullId = insertGame(owner.userId, end.minus(2, ChronoUnit.HOURS), end, GameStatus.FULL);
        UUID inProgressId = insertGame(owner.userId, end.minus(2, ChronoUnit.HOURS), end, GameStatus.IN_PROGRESS);

        scheduler.completeDueGames();

        assertThat(statusOf(fullId)).isEqualTo(GameStatus.COMPLETED);
        assertThat(statusOf(inProgressId)).isEqualTo(GameStatus.COMPLETED);
    }

    @Test
    void scheduler_skipsGameWithinGraceAfterEndTime() throws Exception {
        AuthSession owner = registerAndAuth("s3-owner@example.com", "S3 Owner");
        Instant start = Instant.now().minus(3, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(30, ChronoUnit.MINUTES);
        UUID gameId = insertGame(owner.userId, start, end, GameStatus.OPEN);

        scheduler.completeDueGames();

        assertThat(statusOf(gameId)).isEqualTo(GameStatus.OPEN);
    }

    @Test
    void scheduler_skipsFutureGame() throws Exception {
        AuthSession owner = registerAndAuth("s4-owner@example.com", "S4 Owner");
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        UUID gameId = insertGame(owner.userId, start, start.plus(2, ChronoUnit.HOURS), GameStatus.OPEN);

        scheduler.completeDueGames();

        assertThat(statusOf(gameId)).isEqualTo(GameStatus.OPEN);
    }

    @Test
    void scheduler_skipsCompletedAndCancelledGames() throws Exception {
        AuthSession owner = registerAndAuth("s5-owner@example.com", "S5 Owner");
        Instant end = Instant.now().minus(7, ChronoUnit.HOURS);
        UUID completedId = insertGame(owner.userId, end.minus(2, ChronoUnit.HOURS), end, GameStatus.COMPLETED);
        UUID cancelledId = insertGame(owner.userId, end.minus(2, ChronoUnit.HOURS), end, GameStatus.CANCELLED);

        scheduler.completeDueGames();

        assertThat(statusOf(completedId)).isEqualTo(GameStatus.COMPLETED);
        assertThat(statusOf(cancelledId)).isEqualTo(GameStatus.CANCELLED);
    }

    @Test
    void completeGame_respectsGracePeriod_cannotForceCompletionEarly() throws Exception {
        AuthSession owner = registerAndAuth("g1-owner@example.com", "G1 Owner");
        Instant start = Instant.now().minus(3, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(30, ChronoUnit.MINUTES);
        UUID gameId = insertGame(owner.userId, start, end, GameStatus.OPEN);

        attendanceService.completeGame(gameId);

        assertThat(statusOf(gameId)).isEqualTo(GameStatus.OPEN);
    }

    @Test
    void attendance_afterCompletedGame_returns409_gameAlreadyCompleted() throws Exception {
        AuthSession owner = registerAndAuth("ca-owner@example.com", "CA Owner");
        AuthSession player = registerAndAuth("ca-player@example.com", "CA Player");
        Instant start = Instant.now().minus(9, ChronoUnit.HOURS);
        Instant end = Instant.now().minus(7, ChronoUnit.HOURS);
        UUID gameId = insertGame(owner.userId, start, end, GameStatus.OPEN);
        insertParticipant(gameId, owner.userId, MatchParticipant.Role.OWNER, null);
        insertParticipant(gameId, player.userId, MatchParticipant.Role.PLAYER, null);

        scheduler.completeDueGames();
        String playerRow = matchParticipantRepository.findByGameId(gameId).stream()
                .filter(p -> p.getUserId().equals(player.userId)).findFirst().orElseThrow().getId().toString();

        mockMvc.perform(patch("/api/games/{gameId}/participants/{participantId}/attendance", gameId, playerRow)
                        .cookie(authCookie(owner.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attended\": true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_ALREADY_COMPLETED"));
    }

    // ── stats contributions (attended-only) ─────────────────────────────

    @Test
    void stats_completedGame_attendedCountsMatchesPlayed() throws Exception {
        AuthSession owner = registerAndAuth("st1-owner@example.com", "St1 Owner");
        AuthSession player = registerAndAuth("st1-player@example.com", "St1 Player");
        UUID gameId = insertGame(owner.userId,
                Instant.now().minus(9, ChronoUnit.HOURS),
                Instant.now().minus(7, ChronoUnit.HOURS),
                GameStatus.COMPLETED);
        insertParticipant(gameId, player.userId, MatchParticipant.Role.PLAYER, true);
        insertParticipant(gameId, owner.userId, MatchParticipant.Role.OWNER, true);

        mockMvc.perform(get("/api/users/me").cookie(authCookie(player.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.matchesPlayed").value(1))
                .andExpect(jsonPath("$.stats.matchesCompleted").value(1))
                .andExpect(jsonPath("$.stats.attendanceRate").value(1.0));
    }

    @Test
    void stats_completedGame_notAttendedCountsNothing() throws Exception {
        AuthSession owner = registerAndAuth("st2-owner@example.com", "St2 Owner");
        AuthSession player = registerAndAuth("st2-player@example.com", "St2 Player");
        UUID gameId = insertGame(owner.userId,
                Instant.now().minus(9, ChronoUnit.HOURS),
                Instant.now().minus(7, ChronoUnit.HOURS),
                GameStatus.COMPLETED);
        insertParticipant(gameId, player.userId, MatchParticipant.Role.PLAYER, false);
        insertParticipant(gameId, owner.userId, MatchParticipant.Role.OWNER, true);

        mockMvc.perform(get("/api/users/me").cookie(authCookie(player.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.matchesPlayed").value(0))
                .andExpect(jsonPath("$.stats.matchesCompleted").value(0))
                .andExpect(jsonPath("$.stats.attendanceRate").value(0.0));
    }

    @Test
    void stats_completedGame_unmarkedAttendanceCountsAsNotAttended() throws Exception {
        AuthSession owner = registerAndAuth("st3-owner@example.com", "St3 Owner");
        AuthSession player = registerAndAuth("st3-player@example.com", "St3 Player");
        UUID gameId = insertGame(owner.userId,
                Instant.now().minus(9, ChronoUnit.HOURS),
                Instant.now().minus(7, ChronoUnit.HOURS),
                GameStatus.COMPLETED);
        insertParticipant(gameId, player.userId, MatchParticipant.Role.PLAYER, null);

        mockMvc.perform(get("/api/users/me").cookie(authCookie(player.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.matchesPlayed").value(0))
                .andExpect(jsonPath("$.stats.matchesCompleted").value(0))
                .andExpect(jsonPath("$.stats.attendanceRate").value(0.0));
    }

    @Test
    void stats_nonCompletedGame_attendanceHasNoEffect() throws Exception {
        AuthSession owner = registerAndAuth("st4-owner@example.com", "St4 Owner");
        AuthSession player = registerAndAuth("st4-player@example.com", "St4 Player");
        UUID gameId = insertGame(owner.userId,
                Instant.now().minus(3, ChronoUnit.HOURS),
                Instant.now().minus(30, ChronoUnit.MINUTES),
                GameStatus.OPEN);
        insertParticipant(gameId, player.userId, MatchParticipant.Role.PLAYER, true);

        mockMvc.perform(get("/api/users/me").cookie(authCookie(player.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.matchesPlayed").value(0))
                .andExpect(jsonPath("$.stats.matchesCompleted").value(0))
                .andExpect(jsonPath("$.stats.attendanceRate").doesNotExist());
    }

    @Test
    void stats_completedGame_ownerParticipantCountsWhenAttended() throws Exception {
        AuthSession owner = registerAndAuth("st5-owner@example.com", "St5 Owner");
        UUID gameId = insertGame(owner.userId,
                Instant.now().minus(9, ChronoUnit.HOURS),
                Instant.now().minus(7, ChronoUnit.HOURS),
                GameStatus.COMPLETED);
        insertParticipant(gameId, owner.userId, MatchParticipant.Role.OWNER, true);

        mockMvc.perform(get("/api/users/me").cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.matchesPlayed").value(1))
                .andExpect(jsonPath("$.stats.attendanceRate").value(1.0));
    }
}