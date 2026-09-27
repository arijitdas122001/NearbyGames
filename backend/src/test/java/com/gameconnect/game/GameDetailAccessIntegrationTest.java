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
 * Game-detail visibility.
 *
 * <p>OPEN and FULL games must stay browsable by any authenticated user, because
 * the discovery surface links to them. Once a game leaves that set, it is
 * restricted to the people legitimately associated with it.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class GameDetailAccessIntegrationTest {

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

    private void insertParticipant(UUID gameId, UUID userId, MatchParticipant.Role role) {
        MatchParticipant participant = new MatchParticipant();
        participant.setGameId(gameId);
        participant.setUserId(userId);
        participant.setRole(role);
        participant.setAttended(null);
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

    private Instant pastHoursAgo(long hours) {
        return Instant.now().minus(hours, ChronoUnit.HOURS);
    }

    // ── baseline ────────────────────────────────────────────────────────

    @Test
    void detail_withoutAuth_returns401() throws Exception {
        AuthSession owner = registerAndAuth("da1-owner@example.com", "DA1 Owner");
        UUID gameId = insertGame(owner.userId(), "Anon Turf",
                futureAt(24), futureAt(26), GameStatus.OPEN);

        mockMvc.perform(get("/api/games/{id}", gameId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void detail_unknownGame_returns404() throws Exception {
        AuthSession user = registerAndAuth("da2@example.com", "DA2");
        mockMvc.perform(get("/api/games/{id}", UUID.randomUUID())
                        .cookie(authCookie(user.token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_NOT_FOUND"));
    }

    // ── browsable: OPEN / FULL ──────────────────────────────────────────

    @Test
    void detail_openGame_isBrowsableByAnyAuthenticatedUser() throws Exception {
        AuthSession owner = registerAndAuth("open-owner@example.com", "Open Owner");
        AuthSession stranger = registerAndAuth("open-stranger@example.com", "Open Stranger");
        UUID gameId = insertGame(owner.userId(), "Open Turf",
                futureAt(24), futureAt(26), GameStatus.OPEN);

        mockMvc.perform(get("/api/games/{id}", gameId).cookie(authCookie(stranger.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(gameId.toString()))
                .andExpect(jsonPath("$.turfName").value("Open Turf"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.myParticipation").doesNotExist());
    }

    @Test
    void detail_fullGame_isStillBrowsableByAnyAuthenticatedUser() throws Exception {
        AuthSession owner = registerAndAuth("full-owner@example.com", "Full Owner");
        AuthSession stranger = registerAndAuth("full-stranger@example.com", "Full Stranger");
        UUID gameId = insertGame(owner.userId(), "Full Turf",
                futureAt(24), futureAt(26), GameStatus.FULL);

        mockMvc.perform(get("/api/games/{id}", gameId).cookie(authCookie(stranger.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULL"));
    }

    // ── restricted: owner always wins ───────────────────────────────────

    @Test
    void detail_completedGame_isVisibleToOwner() throws Exception {
        AuthSession owner = registerAndAuth("co-owner@example.com", "CO Owner");
        UUID gameId = insertGame(owner.userId(), "Done Turf",
                pastHoursAgo(9), pastHoursAgo(7), GameStatus.COMPLETED);

        mockMvc.perform(get("/api/games/{id}", gameId).cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(gameId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void detail_cancelledGame_isVisibleToOwner() throws Exception {
        AuthSession owner = registerAndAuth("cx-owner@example.com", "CX Owner");
        UUID gameId = insertGame(owner.userId(), "Dead Turf",
                futureAt(40), futureAt(42), GameStatus.CANCELLED);

        mockMvc.perform(get("/api/games/{id}", gameId).cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void detail_inProgressGame_isVisibleToOwner() throws Exception {
        AuthSession owner = registerAndAuth("ip-owner@example.com", "IP Owner");
        UUID gameId = insertGame(owner.userId(), "Live Turf",
                pastHoursAgo(1), futureAt(1), GameStatus.IN_PROGRESS);

        mockMvc.perform(get("/api/games/{id}", gameId).cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    // ── restricted: participant keeps access ────────────────────────────

    @Test
    void detail_completedGame_isVisibleToParticipant() throws Exception {
        AuthSession owner = registerAndAuth("cp-owner@example.com", "CP Owner");
        AuthSession player = registerAndAuth("cp-player@example.com", "CP Player");
        UUID gameId = insertGame(owner.userId(), "Played Turf",
                pastHoursAgo(9), pastHoursAgo(7), GameStatus.COMPLETED);
        insertParticipant(gameId, owner.userId(), MatchParticipant.Role.OWNER);
        insertParticipant(gameId, player.userId(), MatchParticipant.Role.PLAYER);

        mockMvc.perform(get("/api/games/{id}", gameId).cookie(authCookie(player.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(gameId.toString()))
                .andExpect(jsonPath("$.myParticipation.role").value("PLAYER"));
    }

    @Test
    void detail_cancelledGame_isVisibleToParticipant() throws Exception {
        AuthSession owner = registerAndAuth("cxp-owner@example.com", "CXP Owner");
        AuthSession player = registerAndAuth("cxp-player@example.com", "CXP Player");
        UUID gameId = insertGame(owner.userId(), "Dead Shared",
                futureAt(40), futureAt(42), GameStatus.CANCELLED);
        insertParticipant(gameId, player.userId(), MatchParticipant.Role.PLAYER);

        mockMvc.perform(get("/api/games/{id}", gameId).cookie(authCookie(player.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myParticipation.role").value("PLAYER"));
    }

    // ── restricted: pending requester keeps access ──────────────────────

    @Test
    void detail_pendingRequester_canStillViewRestrictedGame() throws Exception {
        AuthSession owner = registerAndAuth("pr-owner@example.com", "PR Owner");
        AuthSession pending = registerAndAuth("pr-pending@example.com", "PR Pending");
        UUID gameId = insertGame(owner.userId(), "Pending Turf",
                pastHoursAgo(9), pastHoursAgo(7), GameStatus.COMPLETED);
        insertJoinRequest(gameId, pending.userId(), RequestStatus.PENDING);

        mockMvc.perform(get("/api/games/{id}", gameId).cookie(authCookie(pending.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(gameId.toString()));
    }

    // ── restricted: everyone else is refused ────────────────────────────

    @Test
    void detail_completedGame_isHiddenFromStranger() throws Exception {
        AuthSession owner = registerAndAuth("cs-owner@example.com", "CS Owner");
        AuthSession stranger = registerAndAuth("cs-stranger@example.com", "CS Stranger");
        UUID gameId = insertGame(owner.userId(), "Secret Turf",
                pastHoursAgo(9), pastHoursAgo(7), GameStatus.COMPLETED);
        insertParticipant(gameId, owner.userId(), MatchParticipant.Role.OWNER);

        mockMvc.perform(get("/api/games/{id}", gameId).cookie(authCookie(stranger.token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GAME_ACCESS_DENIED"));
    }

    @Test
    void detail_completedGame_isHiddenFromRejectedRequester() throws Exception {
        AuthSession owner = registerAndAuth("rj-owner@example.com", "RJ Owner");
        AuthSession rejected = registerAndAuth("rj-rejected@example.com", "RJ Rejected");
        UUID gameId = insertGame(owner.userId(), "Rejected Turf",
                pastHoursAgo(9), pastHoursAgo(7), GameStatus.COMPLETED);
        insertJoinRequest(gameId, rejected.userId(), RequestStatus.REJECTED);

        mockMvc.perform(get("/api/games/{id}", gameId).cookie(authCookie(rejected.token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GAME_ACCESS_DENIED"));
    }

    @Test
    void detail_completedGame_isHiddenFromCancelledRequester() throws Exception {
        AuthSession owner = registerAndAuth("cj-owner@example.com", "CJ Owner");
        AuthSession cancelled = registerAndAuth("cj-cancelled@example.com", "CJ Cancelled");
        UUID gameId = insertGame(owner.userId(), "Cancelled Req Turf",
                pastHoursAgo(9), pastHoursAgo(7), GameStatus.COMPLETED);
        insertJoinRequest(gameId, cancelled.userId(), RequestStatus.CANCELLED);

        mockMvc.perform(get("/api/games/{id}", gameId).cookie(authCookie(cancelled.token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GAME_ACCESS_DENIED"));
    }

    @Test
    void detail_cancelledGame_isHiddenFromStranger() throws Exception {
        AuthSession owner = registerAndAuth("cxs-owner@example.com", "CXS Owner");
        AuthSession stranger = registerAndAuth("cxs-stranger@example.com", "CXS Stranger");
        UUID gameId = insertGame(owner.userId(), "Cancelled Turf",
                futureAt(40), futureAt(42), GameStatus.CANCELLED);

        mockMvc.perform(get("/api/games/{id}", gameId).cookie(authCookie(stranger.token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GAME_ACCESS_DENIED"));
    }

    @Test
    void detail_inProgressGame_isHiddenFromStranger() throws Exception {
        AuthSession owner = registerAndAuth("ips-owner@example.com", "IPS Owner");
        AuthSession stranger = registerAndAuth("ips-stranger@example.com", "IPS Stranger");
        UUID gameId = insertGame(owner.userId(), "Live Turf",
                pastHoursAgo(1), futureAt(1), GameStatus.IN_PROGRESS);

        mockMvc.perform(get("/api/games/{id}", gameId).cookie(authCookie(stranger.token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GAME_ACCESS_DENIED"));
    }

    /**
     * A stranger must not be able to learn that a restricted game exists at all.
     * 403 rather than 404 is a deliberate trade-off: the association check happens
     * after the lookup, and 403 avoids leaking existence to authenticated users
     * who may learn of the id from a stale link.
     */
    @Test
    void detail_restrictedGame_doesNotLeakParticipantsToStranger() throws Exception {
        AuthSession owner = registerAndAuth("lk-owner@example.com", "LK Owner");
        AuthSession player = registerAndAuth("lk-player@example.com", "LK Player");
        AuthSession stranger = registerAndAuth("lk-stranger@example.com", "LK Stranger");
        UUID gameId = insertGame(owner.userId(), "Leaky Turf",
                pastHoursAgo(9), pastHoursAgo(7), GameStatus.COMPLETED);
        insertParticipant(gameId, player.userId(), MatchParticipant.Role.PLAYER);

        mockMvc.perform(get("/api/games/{id}", gameId).cookie(authCookie(stranger.token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.participants").doesNotExist())
                .andExpect(jsonPath("$.turfAddress").doesNotExist())
                .andExpect(jsonPath("$.owner").doesNotExist());
    }
}
