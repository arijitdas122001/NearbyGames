package com.gameconnect.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.gameconnect.TestcontainersConfiguration;
import com.gameconnect.auth.entity.User;
import com.gameconnect.auth.entity.User.SkillLevel;
import com.gameconnect.auth.repository.UserRepository;
import com.gameconnect.game.dto.CreateGameRequest;
import com.gameconnect.game.dto.DecisionRequest.Decision;
import com.gameconnect.game.dto.GameResponse;
import com.gameconnect.game.dto.JoinRequestResponse;
import com.gameconnect.game.entity.Game;
import com.gameconnect.game.entity.Game.GameFormat;
import com.gameconnect.game.repository.GameRepository;
import com.gameconnect.game.service.GameService;
import com.gameconnect.game.service.JoinRequestService;
import com.gameconnect.notification.entity.Notification;
import com.gameconnect.notification.entity.Notification.NotificationStatus;
import com.gameconnect.notification.entity.Notification.NotificationType;
import com.gameconnect.notification.repository.NotificationRepository;
import com.gameconnect.notification.service.NotificationSchedulingService;
import com.gameconnect.security.JwtAuthenticationFilter.AuthenticatedUser;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NotificationSchedulingIntegrationTest {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    @Autowired
    private NotificationSchedulingService schedulingService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private GameService gameService;

    @Autowired
    private JoinRequestService joinRequestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM notification");
        jdbcTemplate.execute("DELETE FROM player_rating");
        jdbcTemplate.execute("DELETE FROM match_participant");
        jdbcTemplate.execute("DELETE FROM join_request");
        jdbcTemplate.execute("DELETE FROM game");
        jdbcTemplate.execute("DELETE FROM app_user");
    }

    @Test
    void gameCreationSchedulesReminderForOwnerParticipant() {
        User owner = createUser("owner@example.com");
        Instant start = Instant.now().plus(3, ChronoUnit.HOURS);

        GameResponse game = gameService.createGame(principal(owner), createRequest(start));

        Notification reminder = findReminder(game.id(), owner.getId()).orElseThrow();
        assertThat(reminder.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(reminder.getScheduledAt()).isCloseTo(start.minus(1, ChronoUnit.HOURS),
                within(5, ChronoUnit.SECONDS));
    }

    @Test
    void acceptingJoinRequestSchedulesReminderForAcceptedPlayer() {
        User owner = createUser("owner@example.com");
        User player = createUser("player@example.com");
        GameResponse game = gameService.createGame(principal(owner),
                createRequest(Instant.now().plus(3, ChronoUnit.HOURS)));
        JoinRequestResponse request = joinRequestService.createJoinRequest(principal(player), game.id());

        joinRequestService.decideRequest(principal(owner), game.id(), request.id(), Decision.ACCEPT);

        Notification reminder = findReminder(game.id(), player.getId()).orElseThrow();
        assertThat(reminder.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(findReminder(game.id(), owner.getId())).isPresent();
    }

    @Test
    void pendingAndRejectedRequestsDoNotCreateReminders() {
        User owner = createUser("owner@example.com");
        User pending = createUser("pending@example.com");
        User rejected = createUser("rejected@example.com");
        GameResponse game = gameService.createGame(principal(owner),
                createRequest(Instant.now().plus(3, ChronoUnit.HOURS)));
        JoinRequestResponse pendingRequest =
                joinRequestService.createJoinRequest(principal(pending), game.id());
        JoinRequestResponse rejectedRequest =
                joinRequestService.createJoinRequest(principal(rejected), game.id());

        joinRequestService.decideRequest(principal(owner), game.id(), rejectedRequest.id(), Decision.REJECT);

        assertThat(pendingRequest.status()).isEqualTo(
                com.gameconnect.game.entity.JoinRequest.RequestStatus.PENDING);
        assertThat(findReminder(game.id(), pending.getId())).isEmpty();
        assertThat(findReminder(game.id(), rejected.getId())).isEmpty();
    }

    @Test
    void duplicateSchedulingDoesNotCreateDuplicateRecords() {
        User owner = createUser("owner@example.com");
        User player = createUser("player@example.com");
        Game game = createGame(owner.getId(), Instant.now().plus(3, ChronoUnit.HOURS));
        Instant start = game.getStartTime();

        schedulingService.scheduleAcceptedParticipant(game.getId(), player.getId(), start);
        schedulingService.scheduleAcceptedParticipant(game.getId(), player.getId(), start);

        assertThat(notificationRepository.findByGameIdAndUserId(game.getId(), player.getId()))
                .hasSize(1);
    }

    @Test
    void lateAcceptanceIsImmediatelyEligible() {
        User owner = createUser("owner@example.com");
        User player = createUser("player@example.com");
        Game game = createGame(owner.getId(), Instant.now().plus(30, ChronoUnit.MINUTES));
        Instant before = Instant.now();

        schedulingService.scheduleAcceptedParticipant(game.getId(), player.getId(), game.getStartTime());

        Notification reminder = findReminder(game.getId(), player.getId()).orElseThrow();
        assertThat(reminder.getScheduledAt()).isBetween(before.minusSeconds(1), Instant.now().plusSeconds(1));
    }

    @Test
    void participantRemovalCancelsUnsentReminder() {
        User owner = createUser("owner@example.com");
        User player = createUser("player@example.com");
        Game game = createGame(owner.getId(), Instant.now().plus(3, ChronoUnit.HOURS));
        schedulingService.scheduleAcceptedParticipant(game.getId(), player.getId(), game.getStartTime());

        schedulingService.cancelPendingForParticipant(game.getId(), player.getId());

        assertThat(findReminder(game.getId(), player.getId()).orElseThrow().getStatus())
                .isEqualTo(NotificationStatus.CANCELLED);
    }

    @Test
    void gameCancellationCancelsAllPendingReminders() {
        User owner = createUser("owner@example.com");
        User first = createUser("first@example.com");
        User second = createUser("second@example.com");
        Game game = createGame(owner.getId(), Instant.now().plus(3, ChronoUnit.HOURS));
        schedulingService.scheduleAcceptedParticipant(game.getId(), first.getId(), game.getStartTime());
        schedulingService.scheduleAcceptedParticipant(game.getId(), second.getId(), game.getStartTime());

        schedulingService.cancelAllPendingForGame(game.getId());

        assertThat(notificationRepository.findByGameIdAndNotificationType(
                game.getId(), NotificationType.GAME_REMINDER))
                .allSatisfy(notification -> assertThat(notification.getStatus())
                        .isEqualTo(NotificationStatus.CANCELLED));
    }

    @Test
    void gameReschedulingUpdatesPendingReminderTime() {
        User owner = createUser("owner@example.com");
        User player = createUser("player@example.com");
        Game game = createGame(owner.getId(), Instant.now().plus(3, ChronoUnit.HOURS));
        schedulingService.scheduleAcceptedParticipant(game.getId(), player.getId(), game.getStartTime());
        Instant newStart = game.getStartTime().plus(2, ChronoUnit.HOURS);

        schedulingService.reschedulePendingForGame(game.getId(), newStart);

        assertThat(findReminder(game.getId(), player.getId()).orElseThrow().getScheduledAt())
                .isCloseTo(newStart.minus(1, ChronoUnit.HOURS), within(5, ChronoUnit.SECONDS));
    }

    @Test
    void cancellationDoesNotChangeAlreadySentReminder() {
        User owner = createUser("owner@example.com");
        User player = createUser("player@example.com");
        Game game = createGame(owner.getId(), Instant.now().plus(3, ChronoUnit.HOURS));
        schedulingService.scheduleAcceptedParticipant(game.getId(), player.getId(), game.getStartTime());
        Notification sent = findReminder(game.getId(), player.getId()).orElseThrow();
        sent.setStatus(NotificationStatus.SENT);
        notificationRepository.saveAndFlush(sent);

        schedulingService.cancelAllPendingForGame(game.getId());

        assertThat(findReminder(game.getId(), player.getId()).orElseThrow().getStatus())
                .isEqualTo(NotificationStatus.SENT);
    }

    private User createUser(String email) {
        return userRepository.save(new User(email, "password-hash", "Player"));
    }

    private Game createGame(UUID ownerId, Instant start) {
        Game game = new Game();
        game.setOwnerId(ownerId);
        game.setTurfName("Turf Arena");
        game.setTurfAddress("MG Road, Bengaluru");
        game.setLatitude(new BigDecimal("12.9716"));
        game.setLongitude(new BigDecimal("77.5946"));
        game.setGameDate(LocalDate.ofInstant(start, INDIA));
        game.setStartTime(start);
        game.setEndTime(start.plus(2, ChronoUnit.HOURS));
        game.setFormat(GameFormat.FIVE_V5);
        game.setSkillLevel(SkillLevel.BEGINNER);
        game.setMaximumPlayers(10);
        return gameRepository.save(game);
    }

    private CreateGameRequest createRequest(Instant start) {
        return new CreateGameRequest(
                "Turf Arena",
                "MG Road, Bengaluru",
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
    }

    private AuthenticatedUser principal(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail());
    }

    private java.util.Optional<Notification> findReminder(UUID gameId, UUID userId) {
        return notificationRepository.findByGameIdAndUserIdAndNotificationType(
                gameId, userId, NotificationType.GAME_REMINDER);
    }
}
