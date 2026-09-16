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
import com.gameconnect.game.dto.CreateGameRequest;
import com.gameconnect.game.entity.Game;
import com.gameconnect.game.entity.Game.GameFormat;
import com.gameconnect.game.entity.Game.GameStatus;
import com.gameconnect.game.entity.MatchParticipant;
import com.gameconnect.game.repository.GameRepository;
import com.gameconnect.game.repository.MatchParticipantRepository;
import com.gameconnect.security.JwtService;

import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class GameDiscoveryIntegrationTest {

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

    private String createOpenGame(AuthSession auth, String turfName, String turfAddress,
                                  LocalDate date, Instant start, Instant end,
                                  GameFormat format, SkillLevel skill, int maxPlayers) throws Exception {
        CreateGameRequest request = new CreateGameRequest(
                turfName,
                turfAddress,
                new BigDecimal("12.9716"),
                new BigDecimal("77.5946"),
                date,
                start,
                end,
                format,
                skill,
                maxPlayers,
                null,
                0,
                null);
        MvcResult result = mockMvc.perform(post("/api/games")
                        .cookie(authCookie(auth.token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private Game insertGame(UUID ownerId, String turfName, String turfAddress,
                            LocalDate date, Instant start, Instant end,
                            GameFormat format, SkillLevel skill, int maxPlayers,
                            GameStatus status) {
        Game game = new Game();
        game.setOwnerId(ownerId);
        game.setTurfName(turfName);
        game.setTurfAddress(turfAddress);
        game.setLatitude(new BigDecimal("12.9716"));
        game.setLongitude(new BigDecimal("77.5946"));
        game.setGameDate(date);
        game.setStartTime(start);
        game.setEndTime(end);
        game.setFormat(format);
        game.setSkillLevel(skill);
        game.setMaximumPlayers(maxPlayers);
        game.setRequiredPlayers(null);
        game.setJoiningFee(0);
        game.setDescription(null);
        game.setStatus(status);
        return gameRepository.save(game);
    }

    private void insertParticipant(UUID gameId, UUID userId, MatchParticipant.Role role) {
        MatchParticipant participant = new MatchParticipant();
        participant.setGameId(gameId);
        participant.setUserId(userId);
        participant.setRole(role);
        participant.setAttended(null);
        matchParticipantRepository.save(participant);
    }

    // ── listing: happy path ──────────────────────────────────────────────

    @Test
    void listing_returnsOpenUpcomingGamesSortedEarliestFirstWithCounts() throws Exception {
        AuthSession owner = registerAndAuth("discow@example.com", "Disc Owner");
        LocalDate day1 = LocalDate.now(INDIA).plusDays(1);
        LocalDate day2 = LocalDate.now(INDIA).plusDays(2);
        Instant base = Instant.now().plus(1, ChronoUnit.DAYS);

        String later = createOpenGame(owner, "Arena 7", "MG Road", day2,
                base.plusSeconds(7200), base.plusSeconds(10800),
                GameFormat.FIVE_V5, SkillLevel.ADVANCED, 10);
        String earlier = createOpenGame(owner, "Blue Turf", "Koramangala", day1,
                base, base.plusSeconds(3600),
                GameFormat.SIX_V6, SkillLevel.BEGINNER, 12);

        mockMvc.perform(get("/api/games").cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(earlier))
                .andExpect(jsonPath("$.content[0].turfName").value("Blue Turf"))
                .andExpect(jsonPath("$.content[0].currentPlayers").value(1))
                .andExpect(jsonPath("$.content[0].maximumPlayers").value(12))
                .andExpect(jsonPath("$.content[0].spotsRemaining").value(11))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[1].id").value(later))
                .andExpect(jsonPath("$.content[1].turfName").value("Arena 7"));
    }

    // ── listing: exclusions ──────────────────────────────────────────────

    @Test
    void listing_excludesPastGames() throws Exception {
        AuthSession owner = registerAndAuth("pastown@example.com", "Past Owner");
        insertGame(owner.userId, "Old Turf", "Addr",
                LocalDate.now(INDIA).minusDays(1),
                Instant.now().minusSeconds(3600), Instant.now().plusSeconds(3600),
                GameFormat.FIVE_V5, SkillLevel.BEGINNER, 10, GameStatus.OPEN);

        mockMvc.perform(get("/api/games").cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listing_excludesNonOpenGames() throws Exception {
        AuthSession owner = registerAndAuth("nonopen@example.com", "Non Open");
        Instant future = Instant.now().plus(1, ChronoUnit.DAYS);
        LocalDate date = LocalDate.now(INDIA).plusDays(1);

        insertGame(owner.userId, "Full", "A", date, future, future.plusSeconds(3600),
                GameFormat.FIVE_V5, SkillLevel.BEGINNER, 10, GameStatus.FULL);
        insertGame(owner.userId, "InProgress", "B", date, future, future.plusSeconds(3600),
                GameFormat.FIVE_V5, SkillLevel.BEGINNER, 10, GameStatus.IN_PROGRESS);
        insertGame(owner.userId, "Completed", "C", date, future, future.plusSeconds(3600),
                GameFormat.FIVE_V5, SkillLevel.BEGINNER, 10, GameStatus.COMPLETED);
        insertGame(owner.userId, "Cancelled", "D", date, future, future.plusSeconds(3600),
                GameFormat.FIVE_V5, SkillLevel.BEGINNER, 10, GameStatus.CANCELLED);
        insertGame(owner.userId, "Open", "E", date, future.plusSeconds(7200), future.plusSeconds(10800),
                GameFormat.FIVE_V5, SkillLevel.BEGINNER, 10, GameStatus.OPEN);

        mockMvc.perform(get("/api/games").cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].turfName").value("Open"));
    }

    // ── listing: pagination ──────────────────────────────────────────────

    @Test
    void listing_pagination_returnsExpectedPages() throws Exception {
        AuthSession owner = registerAndAuth("pageown@example.com", "Page Owner");
        LocalDate date = LocalDate.now(INDIA).plusDays(1);
        for (int i = 0; i < 5; i++) {
            insertGame(owner.userId, "Turf " + i, "Addr " + i, date,
                    Instant.now().plusSeconds((i + 1) * 7200L),
                    Instant.now().plusSeconds((i + 1) * 7200L + 3600),
                    GameFormat.FIVE_V5, SkillLevel.BEGINNER, 10, GameStatus.OPEN);
        }

        mockMvc.perform(get("/api/games")
                        .param("page", "0").param("size", "2")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/games")
                        .param("page", "2").param("size", "2")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true));

        mockMvc.perform(get("/api/games")
                        .param("page", "99").param("size", "20")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    // ── listing: filters ─────────────────────────────────────────────────

    @Test
    void listing_dateFilter_returnsOnlyGamesOnThatDate() throws Exception {
        AuthSession owner = registerAndAuth("dateown@example.com", "Date Owner");
        LocalDate dayA = LocalDate.now(INDIA).plusDays(1);
        LocalDate dayB = LocalDate.now(INDIA).plusDays(2);
        Instant base = Instant.now().plus(1, ChronoUnit.DAYS);

        createOpenGame(owner, "Turf A", "Addr A", dayA, base, base.plusSeconds(3600),
                GameFormat.FIVE_V5, SkillLevel.BEGINNER, 10);
        createOpenGame(owner, "Turf B", "Addr B", dayB, base.plusSeconds(7200), base.plusSeconds(10800),
                GameFormat.FIVE_V5, SkillLevel.BEGINNER, 10);

        mockMvc.perform(get("/api/games")
                        .param("date", dayB.toString())
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].turfName").value("Turf B"));
    }

    @Test
    void listing_formatFilter_acceptsPublicFormatValues() throws Exception {
        AuthSession owner = registerAndAuth("formatown@example.com", "Format Owner");
        Instant base = Instant.now().plus(1, ChronoUnit.DAYS);
        LocalDate date = LocalDate.now(INDIA).plusDays(1);

        createOpenGame(owner, "Five", "A", date, base, base.plusSeconds(3600),
                GameFormat.FIVE_V5, SkillLevel.BEGINNER, 10);
        createOpenGame(owner, "Six", "B", date, base.plusSeconds(7200), base.plusSeconds(10800),
                GameFormat.SIX_V6, SkillLevel.BEGINNER, 10);

        mockMvc.perform(get("/api/games")
                        .param("format", "6V6")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].turfName").value("Six"));

        mockMvc.perform(get("/api/games")
                        .param("format", "11V11")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listing_formatFilter_rejectsJavaEnumNameAndUnknownValues() throws Exception {
        AuthSession owner = registerAndAuth("badformat@example.com", "Bad Format");

        mockMvc.perform(get("/api/games")
                        .param("format", "FIVE_V5")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/games")
                        .param("format", "10V10")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listing_skillFilter_returnsOnlyMatchingSkill() throws Exception {
        AuthSession owner = registerAndAuth("skillown@example.com", "Skill Owner");
        Instant base = Instant.now().plus(1, ChronoUnit.DAYS);
        LocalDate date = LocalDate.now(INDIA).plusDays(1);

        createOpenGame(owner, "Beginner Game", "A", date, base, base.plusSeconds(3600),
                GameFormat.FIVE_V5, SkillLevel.BEGINNER, 10);
        createOpenGame(owner, "Advanced Game", "B", date, base.plusSeconds(7200), base.plusSeconds(10800),
                GameFormat.FIVE_V5, SkillLevel.ADVANCED, 10);

        mockMvc.perform(get("/api/games")
                        .param("skillLevel", "ADVANCED")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].turfName").value("Advanced Game"));

        mockMvc.perform(get("/api/games")
                        .param("skillLevel", "NOPE")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listing_textSearch_matchesTurfNameAndAddressCaseInsensitively() throws Exception {
        AuthSession owner = registerAndAuth("searchown@example.com", "Search Owner");
        Instant base = Instant.now().plus(1, ChronoUnit.DAYS);
        LocalDate date = LocalDate.now(INDIA).plusDays(1);

        createOpenGame(owner, "Arena 7", "MG Road", date, base, base.plusSeconds(3600),
                GameFormat.FIVE_V5, SkillLevel.BEGINNER, 10);
        createOpenGame(owner, "BlueTurf", "Koramangala", date, base.plusSeconds(7200), base.plusSeconds(10800),
                GameFormat.FIVE_V5, SkillLevel.BEGINNER, 10);

        mockMvc.perform(get("/api/games")
                        .param("q", "mg road")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].turfName").value("Arena 7"));

        mockMvc.perform(get("/api/games")
                        .param("q", "ARENA")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].turfName").value("Arena 7"));

        mockMvc.perform(get("/api/games")
                        .param("q", "xyzzy")
                        .cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ── listing: invalid parameter values → 400 ──────────────────────────

    @Test
    void listing_invalidPagination_returns400() throws Exception {
        AuthSession owner = registerAndAuth("badpage@example.com", "Bad Page");
        mockMvc.perform(get("/api/games").param("page", "-1").cookie(authCookie(owner.token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/games").param("size", "0").cookie(authCookie(owner.token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/games").param("size", "101").cookie(authCookie(owner.token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/games").param("page", "abc").cookie(authCookie(owner.token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/games").param("size", "abc").cookie(authCookie(owner.token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listing_invalidDate_returns400() throws Exception {
        AuthSession owner = registerAndAuth("baddate@example.com", "Bad Date");
        mockMvc.perform(get("/api/games").param("date", "2026-13-40").cookie(authCookie(owner.token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/games").param("date", "not-a-date").cookie(authCookie(owner.token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── detail ───────────────────────────────────────────────────────────

    @Test
    void detail_returnsFullGameWithOwnerAndCounts() throws Exception {
        AuthSession owner = registerAndAuth("detailown@example.com", "Detail Owner");
        Instant base = Instant.now().plus(1, ChronoUnit.DAYS);
        LocalDate date = LocalDate.now(INDIA).plusDays(1);
        String gameId = createOpenGame(owner, "Arena 7", "MG Road", date,
                base, base.plusSeconds(3600),
                GameFormat.FIVE_V5, SkillLevel.ADVANCED, 10);

        mockMvc.perform(get("/api/games/{id}", gameId).cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(gameId))
                .andExpect(jsonPath("$.owner.id").value(owner.userId.toString()))
                .andExpect(jsonPath("$.owner.displayName").value("Detail Owner"))
                .andExpect(jsonPath("$.owner.skillLevel").value("BEGINNER"))
                .andExpect(jsonPath("$.turfName").value("Arena 7"))
                .andExpect(jsonPath("$.turfAddress").value("MG Road"))
                .andExpect(jsonPath("$.format").value("5V5"))
                .andExpect(jsonPath("$.skillLevel").value("ADVANCED"))
                .andExpect(jsonPath("$.maximumPlayers").value(10))
                .andExpect(jsonPath("$.currentPlayers").value(1))
                .andExpect(jsonPath("$.spotsRemaining").value(9))
                .andExpect(jsonPath("$.joiningFee").value(0))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void detail_currentPlayersReflectsAllParticipants() throws Exception {
        AuthSession owner = registerAndAuth("partown@example.com", "Part Owner");
        AuthSession player1 = registerAndAuth("player1@example.com", "Player One");
        AuthSession player2 = registerAndAuth("player2@example.com", "Player Two");
        Instant base = Instant.now().plus(1, ChronoUnit.DAYS);
        LocalDate date = LocalDate.now(INDIA).plusDays(1);
        String gameId = createOpenGame(owner, "Turf", "Addr", date,
                base, base.plusSeconds(3600),
                GameFormat.FIVE_V5, SkillLevel.BEGINNER, 12);

        UUID gameUuid = UUID.fromString(gameId);
        insertParticipant(gameUuid, player1.userId, MatchParticipant.Role.PLAYER);
        insertParticipant(gameUuid, player2.userId, MatchParticipant.Role.PLAYER);

        mockMvc.perform(get("/api/games/{id}", gameId).cookie(authCookie(owner.token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPlayers").value(3))
                .andExpect(jsonPath("$.spotsRemaining").value(9));
    }

    @Test
    void detail_gameNotFound_returns404() throws Exception {
        AuthSession owner = registerAndAuth("nfown@example.com", "Not Found");
        mockMvc.perform(get("/api/games/{id}", UUID.randomUUID()).cookie(authCookie(owner.token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_NOT_FOUND"));
    }

    // ── auth ─────────────────────────────────────────────────────────────

    @Test
    void listing_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/games"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void detail_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/games/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}