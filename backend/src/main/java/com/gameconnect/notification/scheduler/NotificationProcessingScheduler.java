package com.gameconnect.notification.scheduler;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.gameconnect.notification.email.ResendEmailProvider;
import com.gameconnect.notification.service.NotificationClaimService;
import com.gameconnect.notification.service.NotificationDeliveryService;

@Component
@ConditionalOnProperty(name = "notifications.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class NotificationProcessingScheduler {

    private final NotificationClaimService claimService;
    private final NotificationDeliveryService deliveryService;
    private static final Logger log = LoggerFactory.getLogger(ResendEmailProvider.class);

    public NotificationProcessingScheduler(NotificationClaimService claimService,
                                           NotificationDeliveryService deliveryService) {
        this.claimService = claimService;
        this.deliveryService = deliveryService;
    }

    @Scheduled(fixedDelayString = "${notifications.poll-interval:10s}", initialDelayString = "${notifications.poll-interval:10s}")
    public void processDueNotifications() {
        Instant now = Instant.now();
        log.info("My notification scheduling servince is running");
        claimService.recoverStaleClaims(now);
        List<UUID> claimed = claimService.claimDue(now);
        for (UUID notificationId : claimed) {
            deliveryService.process(notificationId);
        }
    }
}
