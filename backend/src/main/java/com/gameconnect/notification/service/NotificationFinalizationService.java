package com.gameconnect.notification.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.gameconnect.notification.config.NotificationProperties;
import com.gameconnect.notification.repository.NotificationClaimRepository;
import com.gameconnect.notification.repository.NotificationClaimRepository.FailureResult;

@Service
public class NotificationFinalizationService {

    private static final long MAX_DELAY_MILLIS = Duration.ofHours(1).toMillis();
    private static final int MAX_ERROR_LENGTH = 1000;

    private final NotificationClaimRepository repository;
    private final NotificationProperties properties;

    public NotificationFinalizationService(NotificationClaimRepository repository,
                                           NotificationProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(UUID notificationId, String providerMessageId) {
        Instant now = Instant.now();
        repository.markSent(notificationId, providerMessageId, now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FailureResult recordFailure(UUID notificationId, Throwable failure, boolean retryable) {
        int attempts = repository.attemptCount(notificationId);
        Instant nextAttemptAt = Instant.now().plus(backoff(attempts));
        return repository.finalizeFailure(
                notificationId,
                describe(failure),
                retryable,
                nextAttemptAt,
                properties.getMaxAttempts(),
                Instant.now());
    }

    private Duration backoff(int attempt) {
        long base = Math.min(MAX_DELAY_MILLIS,
                Math.max(1L, properties.getBaseRetryDelay().toMillis()));
        int shift = Math.min(Math.max(attempt - 1, 0), 10);
        long delay = Math.min(MAX_DELAY_MILLIS, base << shift);
        long jitterBound = Math.max(1L, delay / 10);
        long jitter = ThreadLocalRandom.current().nextLong(-jitterBound, jitterBound + 1);
        return Duration.ofMillis(Math.max(0L, delay + jitter));
    }

    private String describe(Throwable failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            message = failure.getClass().getSimpleName();
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() <= MAX_ERROR_LENGTH
                ? normalized
                : normalized.substring(0, MAX_ERROR_LENGTH);
    }
}
