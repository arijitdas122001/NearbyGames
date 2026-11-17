package com.gameconnect.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.gameconnect.TestcontainersConfiguration;
import com.gameconnect.auth.entity.User;
import com.gameconnect.auth.entity.User.SkillLevel;
import com.gameconnect.auth.repository.UserRepository;
import com.gameconnect.game.entity.Game;
import com.gameconnect.game.entity.Game.GameFormat;
import com.gameconnect.game.repository.GameRepository;
import com.gameconnect.notification.entity.Notification;
import com.gameconnect.notification.entity.Notification.NotificationStatus;
import com.gameconnect.notification.entity.Notification.NotificationType;
import com.gameconnect.notification.repository.NotificationRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NotificationRepositoryIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

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
        jdbcTemplate.execute("DELETE FROM game");
        jdbcTemplate.execute("DELETE FROM app_user");
    }

    private User createUser(String email) {
        return userRepository.save(new User(email, "password-hash", "Player"));
    }

    private Game createGame(UUID ownerId) {
        Game game = new Game();
        game.setOwnerId(ownerId);
        game.setTurfName("Turf Arena");
        game.setTurfAddress("MG Road, Bengaluru");
        game.setLatitude(new BigDecimal("12.9716"));
        game.setLongitude(new BigDecimal("77.5946"));
        game.setGameDate(LocalDate.now().plusDays(2));
        game.setStartTime(Instant.now().plusSeconds(3600));
        game.setEndTime(Instant.now().plusSeconds(7200));
        game.setFormat(GameFormat.FIVE_V5);
        game.setSkillLevel(SkillLevel.BEGINNER);
        game.setMaximumPlayers(10);
        return gameRepository.save(game);
    }

    private Notification newNotification(UUID gameId, UUID userId, Instant scheduledAt) {
        Notification notification = new Notification();
        notification.setGameId(gameId);
        notification.setUserId(userId);
        notification.setNotificationType(NotificationType.GAME_REMINDER);
        notification.setScheduledAt(scheduledAt);
        return notification;
    }

    @Test
    void persistsNotificationWithGeneratedIdAndDefaults() {
        User user = createUser("persist@example.com");
        Game game = createGame(user.getId());
        Instant scheduledAt = Instant.now().plusSeconds(600).truncatedTo(ChronoUnit.MILLIS);

        Notification saved = notificationRepository.saveAndFlush(
                newNotification(game.getId(), user.getId(), scheduledAt));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(saved.getAttemptCount()).isZero();
        assertThat(saved.getSentAt()).isNull();
        assertThat(saved.getProviderMessageId()).isNull();
        assertThat(saved.getLastError()).isNull();

        Notification found = notificationRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getGameId()).isEqualTo(game.getId());
        assertThat(found.getUserId()).isEqualTo(user.getId());
        assertThat(found.getNotificationType()).isEqualTo(NotificationType.GAME_REMINDER);
        assertThat(found.getScheduledAt()).isEqualTo(scheduledAt);
    }

    @Test
    void uniqueConstraintRejectsDuplicateGameUserAndType() {
        User user = createUser("duplicate@example.com");
        Game game = createGame(user.getId());
        Instant scheduledAt = Instant.now().plusSeconds(600);

        notificationRepository.saveAndFlush(newNotification(game.getId(), user.getId(), scheduledAt));

        assertThatThrownBy(() -> notificationRepository.saveAndFlush(
                newNotification(game.getId(), user.getId(), scheduledAt.plusSeconds(60))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void foreignKeyRejectsUnknownGame() {
        User user = createUser("unknown-game@example.com");

        Notification orphan = newNotification(UUID.randomUUID(), user.getId(), Instant.now());

        assertThatThrownBy(() -> notificationRepository.saveAndFlush(orphan))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void foreignKeyRejectsUnknownUser() {
        User owner = createUser("unknown-user@example.com");
        Game game = createGame(owner.getId());

        Notification orphan = newNotification(game.getId(), UUID.randomUUID(), Instant.now());

        assertThatThrownBy(() -> notificationRepository.saveAndFlush(orphan))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsNotificationsByGameAndUser() {
        User user = createUser("lookup@example.com");
        Game game = createGame(user.getId());
        Notification notification = notificationRepository.saveAndFlush(
                newNotification(game.getId(), user.getId(), Instant.now().plusSeconds(600)));

        assertThat(notificationRepository.findByGameIdAndUserId(game.getId(), user.getId()))
                .extracting(Notification::getId)
                .containsExactly(notification.getId());
        assertThat(notificationRepository.findByGameIdAndUserIdAndNotificationType(
                game.getId(), user.getId(), NotificationType.GAME_REMINDER))
                .map(Notification::getId)
                .hasValue(notification.getId());
    }

    @Test
    void findsNotificationsByStatus() {
        User user = createUser("status@example.com");
        Game firstGame = createGame(user.getId());
        Game secondGame = createGame(user.getId());

        Notification pending = notificationRepository.saveAndFlush(
                newNotification(firstGame.getId(), user.getId(), Instant.now().plusSeconds(600)));

        Notification sent = newNotification(secondGame.getId(), user.getId(), Instant.now());
        sent.setStatus(NotificationStatus.SENT);
        sent.setSentAt(Instant.now());
        sent.setProviderMessageId("msg_test");
        notificationRepository.saveAndFlush(sent);

        assertThat(notificationRepository.findByStatus(NotificationStatus.PENDING))
                .extracting(Notification::getId)
                .containsExactly(pending.getId());
        assertThat(notificationRepository.findByStatus(NotificationStatus.SENT))
                .extracting(Notification::getId)
                .containsExactly(sent.getId());
    }

    @Test
    void findsDuePendingNotificationsOldestFirst() {
        User user = createUser("due@example.com");
        Game firstGame = createGame(user.getId());
        Game secondGame = createGame(user.getId());
        Game thirdGame = createGame(user.getId());
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        Notification later = notificationRepository.saveAndFlush(
                newNotification(firstGame.getId(), user.getId(), now.minusSeconds(60)));
        Notification earlier = notificationRepository.saveAndFlush(
                newNotification(secondGame.getId(), user.getId(), now.minusSeconds(600)));

        Notification future = newNotification(thirdGame.getId(), user.getId(), now.plusSeconds(600));
        notificationRepository.saveAndFlush(future);

        List<Notification> due = notificationRepository
                .findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                        NotificationStatus.PENDING, now);

        assertThat(due).extracting(Notification::getId)
                .containsExactly(earlier.getId(), later.getId());
    }
}
