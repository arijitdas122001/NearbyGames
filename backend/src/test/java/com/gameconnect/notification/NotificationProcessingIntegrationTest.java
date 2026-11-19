package com.gameconnect.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.gameconnect.TestcontainersConfiguration;
import com.gameconnect.auth.entity.User;
import com.gameconnect.auth.entity.User.SkillLevel;
import com.gameconnect.auth.repository.UserRepository;
import com.gameconnect.game.entity.Game;
import com.gameconnect.game.entity.Game.GameFormat;
import com.gameconnect.game.repository.GameRepository;
import com.gameconnect.notification.email.EmailDeliveryException;
import com.gameconnect.notification.email.EmailMessage;
import com.gameconnect.notification.email.EmailProvider;
import com.gameconnect.notification.entity.Notification;
import com.gameconnect.notification.entity.Notification.NotificationStatus;
import com.gameconnect.notification.entity.Notification.NotificationType;
import com.gameconnect.notification.repository.NotificationRepository;
import com.gameconnect.notification.scheduler.NotificationProcessingScheduler;
import com.gameconnect.notification.service.NotificationClaimService;
import com.gameconnect.notification.service.NotificationDeliveryService;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NotificationProcessingIntegrationTest {

    @Autowired
    private NotificationClaimService claimService;

    @Autowired
    private NotificationDeliveryService deliveryService;

    @Autowired
    private NotificationProcessingScheduler scheduler;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private EmailProvider emailProvider;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM notification");
        jdbcTemplate.execute("DELETE FROM game");
        jdbcTemplate.execute("DELETE FROM app_user");
    }

    @Test
    void dueNotificationIsClaimedAndSentWithNoOpenTransaction() {
        User user = createUser("player@example.com");
        Game game = createGame(user.getId(), Instant.now().plus(2, ChronoUnit.HOURS));
        Notification notification = createNotification(game.getId(), user.getId(),
                Instant.now().minusSeconds(30), NotificationStatus.PENDING, 0);
        AtomicBoolean checked = new AtomicBoolean();

        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            String status = jdbcTemplate.queryForObject(
                    "SELECT status FROM notification WHERE id = ?", String.class, notification.getId());
            assertThat(status).isEqualTo("PROCESSING");
            checked.set(true);
            return "msg_123";
        }).when(emailProvider).send(any(EmailMessage.class));

        Instant before = Instant.now();
        assertThat(claimService.claimDue(before)).containsExactly(notification.getId());
        deliveryService.process(notification.getId());

        Notification updated = find(notification.getId());
        assertThat(updated.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(updated.getSentAt()).isBetween(before.minusSeconds(1), Instant.now().plusSeconds(1));
        assertThat(updated.getProviderMessageId()).isEqualTo("msg_123");
        assertThat(updated.getAttemptCount()).isEqualTo(1);
        assertThat(updated.getLastError()).isNull();
        assertThat(checked).isTrue();

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailProvider).send(captor.capture());
        EmailMessage message = captor.getValue();
        assertThat(message.to()).isEqualTo(user.getEmail());
        assertThat(message.subject()).contains("Turf Arena");
        assertThat(message.text()).contains("Turf Arena", "MG Road, Bengaluru", "5V5");
    }

    @Test
    void futureNotificationIsNotProcessed() {
        User user = createUser("future@example.com");
        Game game = createGame(user.getId(), Instant.now().plus(4, ChronoUnit.HOURS));
        Notification notification = createNotification(game.getId(), user.getId(),
                Instant.now().plus(2, ChronoUnit.HOURS), NotificationStatus.PENDING, 0);

        scheduler.processDueNotifications();

        assertThat(find(notification.getId()).getStatus()).isEqualTo(NotificationStatus.PENDING);
        verifyNoInteractions(emailProvider);
    }

    @Test
    void transientProviderFailureSchedulesRetry() {
        User user = createUser("retry@example.com");
        Game game = createGame(user.getId(), Instant.now().plus(2, ChronoUnit.HOURS));
        Notification notification = createNotification(game.getId(), user.getId(),
                Instant.now().minusSeconds(30), NotificationStatus.PENDING, 0);
        doThrow(new EmailDeliveryException("temporary provider error",
                new IllegalStateException("network"), true))
                .when(emailProvider).send(any(EmailMessage.class));

        Instant before = Instant.now();
        assertThat(claimService.claimDue(before)).containsExactly(notification.getId());
        deliveryService.process(notification.getId());

        Notification updated = find(notification.getId());
        assertThat(updated.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(updated.getAttemptCount()).isEqualTo(1);
        assertThat(updated.getLastError()).contains("temporary provider error");
        assertThat(updated.getScheduledAt()).isAfter(before);
    }

    @Test
    void permanentProviderFailureMarksNotificationFailed() {
        User user = createUser("permanent@example.com");
        Game game = createGame(user.getId(), Instant.now().plus(2, ChronoUnit.HOURS));
        Notification notification = createNotification(game.getId(), user.getId(),
                Instant.now().minusSeconds(30), NotificationStatus.PENDING, 0);
        doThrow(new EmailDeliveryException("invalid sender", false))
                .when(emailProvider).send(any(EmailMessage.class));

        claimService.claimDue(Instant.now());
        deliveryService.process(notification.getId());

        Notification updated = find(notification.getId());
        assertThat(updated.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(updated.getAttemptCount()).isEqualTo(1);
        assertThat(updated.getLastError()).contains("invalid sender");
    }

    @Test
    void maximumAttemptsAreRespected() {
        User user = createUser("max@example.com");
        Game game = createGame(user.getId(), Instant.now().plus(2, ChronoUnit.HOURS));
        Notification notification = createNotification(game.getId(), user.getId(),
                Instant.now().minusSeconds(30), NotificationStatus.PENDING, 2);
        doThrow(new EmailDeliveryException("temporary provider error",
                new IllegalStateException("network"), true))
                .when(emailProvider).send(any(EmailMessage.class));

        claimService.claimDue(Instant.now());
        deliveryService.process(notification.getId());

        Notification updated = find(notification.getId());
        assertThat(updated.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(updated.getAttemptCount()).isEqualTo(3);
    }

    @Test
    void staleProcessingBelowLimitIsRequeued() {
        User user = createUser("stale@example.com");
        Game game = createGame(user.getId(), Instant.now().plus(2, ChronoUnit.HOURS));
        Notification notification = createNotification(game.getId(), user.getId(),
                Instant.now().minusSeconds(30), NotificationStatus.PROCESSING, 1);
        jdbcTemplate.update("UPDATE notification SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minus(30, ChronoUnit.MINUTES)), notification.getId());

        Instant before = Instant.now();
        assertThat(claimService.recoverStaleClaims(before)).containsExactly(notification.getId());

        Notification updated = find(notification.getId());
        assertThat(updated.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(updated.getScheduledAt()).isBetween(before.minusSeconds(1), before.plusSeconds(1));
        assertThat(updated.getLastError()).contains("recovered");
    }

    @Test
    void staleProcessingAtLimitIsFailed() {
        User user = createUser("stale-max@example.com");
        Game game = createGame(user.getId(), Instant.now().plus(2, ChronoUnit.HOURS));
        Notification notification = createNotification(game.getId(), user.getId(),
                Instant.now().minusSeconds(30), NotificationStatus.PROCESSING, 3);
        jdbcTemplate.update("UPDATE notification SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minus(30, ChronoUnit.MINUTES)), notification.getId());

        assertThat(claimService.recoverStaleClaims(Instant.now())).containsExactly(notification.getId());

        Notification updated = find(notification.getId());
        assertThat(updated.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(updated.getLastError()).contains("exhausted retry limit");
    }

    @Test
    void cancelledAndSentNotificationsAreNotClaimed() {
        User user = createUser("terminal@example.com");
        Game cancelledGame = createGame(user.getId(), Instant.now().plus(2, ChronoUnit.HOURS));
        Game sentGame = createGame(user.getId(), Instant.now().plus(3, ChronoUnit.HOURS));
        Notification cancelled = createNotification(cancelledGame.getId(), user.getId(),
                Instant.now().minusSeconds(30), NotificationStatus.CANCELLED, 0);
        Notification sent = createNotification(sentGame.getId(), user.getId(),
                Instant.now().minusSeconds(30), NotificationStatus.SENT, 1);

        scheduler.processDueNotifications();

        assertThat(find(cancelled.getId()).getStatus()).isEqualTo(NotificationStatus.CANCELLED);
        assertThat(find(sent.getId()).getStatus()).isEqualTo(NotificationStatus.SENT);
        verifyNoInteractions(emailProvider);
    }

    @Test
    void skipLockedPreventsTwoWorkersClaimingSameNotification() throws Exception {
        User user = createUser("concurrency@example.com");
        Game game = createGame(user.getId(), Instant.now().plus(2, ChronoUnit.HOURS));
        Notification notification = createNotification(game.getId(), user.getId(),
                Instant.now().minusSeconds(30), NotificationStatus.PENDING, 0);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            new TransactionTemplate(transactionManager).execute(status -> {
                jdbcTemplate.queryForObject(
                        "SELECT id FROM notification WHERE id = ? FOR UPDATE", UUID.class,
                        notification.getId());
                try {
                    Future<List<UUID>> secondWorker = executor.submit(
                            () -> claimService.claimDue(Instant.now()));
                    assertThat(secondWorker.get(5, TimeUnit.SECONDS)).doesNotContain(notification.getId());
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
                return null;
            });

            assertThat(claimService.claimDue(Instant.now())).containsExactly(notification.getId());
        } finally {
            executor.shutdownNow();
        }
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
        game.setGameDate(LocalDate.ofInstant(start, java.time.ZoneId.of("Asia/Kolkata")));
        game.setStartTime(start);
        game.setEndTime(start.plus(2, ChronoUnit.HOURS));
        game.setFormat(GameFormat.FIVE_V5);
        game.setSkillLevel(SkillLevel.BEGINNER);
        game.setMaximumPlayers(10);
        return gameRepository.save(game);
    }

    private Notification createNotification(UUID gameId, UUID userId, Instant scheduledAt,
                                            NotificationStatus status, int attempts) {
        Notification notification = new Notification();
        notification.setGameId(gameId);
        notification.setUserId(userId);
        notification.setNotificationType(NotificationType.GAME_REMINDER);
        notification.setScheduledAt(scheduledAt);
        notification.setStatus(status);
        notification.setAttemptCount(attempts);
        return notificationRepository.saveAndFlush(notification);
    }

    private Notification find(UUID id) {
        return notificationRepository.findById(id).orElseThrow();
    }
}
