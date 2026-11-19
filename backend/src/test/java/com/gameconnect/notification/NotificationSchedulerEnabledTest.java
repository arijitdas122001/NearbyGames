package com.gameconnect.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.gameconnect.notification.scheduler.NotificationProcessingScheduler;
import com.gameconnect.notification.service.NotificationClaimService;
import com.gameconnect.notification.service.NotificationDeliveryService;

class NotificationSchedulerEnabledTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(NotificationClaimService.class, () -> mock(NotificationClaimService.class))
            .withBean(NotificationDeliveryService.class, () -> mock(NotificationDeliveryService.class))
            .withUserConfiguration(NotificationProcessingScheduler.class);

    @Test
    void notificationSchedulerCanBeDisabledIndependently() {
        runner.withPropertyValues("notifications.scheduler-enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(NotificationProcessingScheduler.class);
                });
    }

    @Test
    void notificationSchedulerIsEnabledByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(NotificationProcessingScheduler.class);
        });
    }
}
