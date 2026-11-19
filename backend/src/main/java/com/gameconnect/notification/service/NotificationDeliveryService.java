package com.gameconnect.notification.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.gameconnect.notification.email.EmailDeliveryException;
import com.gameconnect.notification.email.EmailMessage;
import com.gameconnect.notification.email.EmailProvider;
import com.gameconnect.notification.repository.NotificationClaimRepository;
import com.gameconnect.notification.repository.NotificationClaimRepository.DeliveryTarget;

@Service
public class NotificationDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryService.class);
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter START_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private final NotificationClaimRepository repository;
    private final EmailProvider emailProvider;
    private final NotificationFinalizationService finalizationService;

    public NotificationDeliveryService(NotificationClaimRepository repository,
                                       EmailProvider emailProvider,
                                       NotificationFinalizationService finalizationService) {
        this.repository = repository;
        this.emailProvider = emailProvider;
        this.finalizationService = finalizationService;
    }

    public void process(UUID notificationId) {
        Optional<DeliveryTarget> target = repository.findDeliveryTarget(notificationId);
        if (target.isEmpty()) {
            log.warn("Reminder {} is no longer claimable", notificationId);
            return;
        }

        try {
            String providerMessageId = emailProvider.send(buildMessage(target.get()));
            if (providerMessageId == null || providerMessageId.isBlank()) {
                throw new EmailDeliveryException("Email provider returned no message id", false);
            }
            finalizationService.markSent(notificationId, providerMessageId);
        } catch (RuntimeException failure) {
            boolean retryable = !(failure instanceof EmailDeliveryException deliveryFailure)
                    || deliveryFailure.isRetryable();
            finalizationService.recordFailure(notificationId, failure, retryable);
        }
    }

    private EmailMessage buildMessage(DeliveryTarget target) {
        String subject = "GameConnect reminder: " + target.turfName() + " starts soon";
        String start = ZonedDateTime.ofInstant(target.startTime(), DISPLAY_ZONE).format(START_FORMAT);
        String location = target.turfAddress() == null || target.turfAddress().isBlank()
                ? "Not provided"
                : target.turfAddress();
        String format = target.format() == null || target.format().isBlank()
                ? "Not specified"
                : target.format();

        String body = """
                Hi,

                This is a reminder that your GameConnect game starts in about one hour.

                Game: %s
                Starts: %s
                Game date: %s
                Location: %s
                Format: %s

                See you there,
                GameConnect
                """.formatted(target.turfName(), start, target.gameDate(), location, format);

        return new EmailMessage(target.email(), subject, body, null);
    }
}
