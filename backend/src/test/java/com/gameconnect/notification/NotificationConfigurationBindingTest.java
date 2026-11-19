package com.gameconnect.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.gameconnect.TestcontainersConfiguration;
import com.gameconnect.auth.entity.User;
import com.gameconnect.auth.entity.User.SkillLevel;
import com.gameconnect.auth.repository.UserRepository;
import com.gameconnect.game.entity.Game;
import com.gameconnect.game.entity.Game.GameFormat;
import com.gameconnect.game.repository.GameRepository;
import com.gameconnect.notification.config.NotificationProperties;
import com.gameconnect.notification.email.EmailProvider;
import com.gameconnect.notification.entity.Notification;
import com.gameconnect.notification.entity.Notification.NotificationType;
import com.gameconnect.notification.repository.NotificationRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "notifications.scheduler-enabled=true",
        "notifications.poll-interval=250ms",
        "notifications.batch-size=7",
        "notifications.max-attempts=4",
        "notifications.base-retry-delay=2s",
        "notifications.stale-processing-timeout=3m"
})
class NotificationConfigurationBindingTest {

    @Autowired
    private NotificationProperties properties;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void bindsAllNotificationProperties() {
        assertThat(properties.isSchedulerEnabled()).isTrue();
        assertThat(properties.getPollInterval()).isEqualTo(Duration.ofMillis(250));
        assertThat(properties.getBatchSize()).isEqualTo(7);
        assertThat(properties.getMaxAttempts()).isEqualTo(4);
        assertThat(properties.getBaseRetryDelay()).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.getStaleProcessingTimeout()).isEqualTo(Duration.ofMinutes(3));
    }

    @Test
    void defaultNotificationPropertiesComeFromApplicationYml() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(NotificationProperties.class)
                .run(context -> {
                    NotificationProperties defaults = context.getBean(NotificationProperties.class);
                    assertThat(defaults.isSchedulerEnabled()).isTrue();
                    assertThat(defaults.getPollInterval()).isEqualTo(Duration.ofSeconds(10));
                    assertThat(defaults.getBatchSize()).isEqualTo(50);
                    assertThat(defaults.getMaxAttempts()).isEqualTo(3);
                    assertThat(defaults.getBaseRetryDelay()).isEqualTo(Duration.ofMinutes(1));
                    assertThat(defaults.getStaleProcessingTimeout()).isEqualTo(Duration.ofMinutes(10));
                });
    }

    @Test
    void globalSchedulingDisabledPreventsAutomaticExecution() throws Exception {
        User user = userRepository.save(new User("auto@example.com", "password-hash", "Player"));
        Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
        Game game = new Game();
        game.setOwnerId(user.getId());
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
        game = gameRepository.save(game);

        Notification notification = new Notification();
        notification.setGameId(game.getId());
        notification.setUserId(user.getId());
        notification.setNotificationType(NotificationType.GAME_REMINDER);
        notification.setScheduledAt(Instant.now().minusSeconds(10));
        notificationRepository.saveAndFlush(notification);

        Thread.sleep(1000);

        assertThat(notificationRepository.findById(notification.getId()).orElseThrow().getStatus())
                .isEqualTo(com.gameconnect.notification.entity.Notification.NotificationStatus.PENDING);
        verifyNoInteractions(emailProvider);
    }
}
