package com.gameconnect.notification.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.gameconnect.notification.config.NotificationProperties;
import com.gameconnect.notification.repository.NotificationClaimRepository;
import com.gameconnect.notification.repository.NotificationClaimRepository.StaleClaim;

@Service
public class NotificationClaimService {

    private static final String STALE_RECOVERY_ERROR = "Stale PROCESSING claim recovered";
    private static final String STALE_EXHAUSTED_ERROR = "Stale PROCESSING claim exhausted retry limit";

    private final NotificationClaimRepository repository;
    private final NotificationProperties properties;

    public NotificationClaimService(NotificationClaimRepository repository,
                                    NotificationProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<UUID> recoverStaleClaims(Instant now) {
        int maxAttempts = properties.getMaxAttempts();
        Instant staleBefore = now.minus(properties.getStaleProcessingTimeout());
        List<StaleClaim> stale = repository.findStaleClaims(staleBefore, properties.getBatchSize());

        List<UUID> retryable = stale.stream()
                .filter(claim -> claim.attemptCount() < maxAttempts)
                .map(StaleClaim::id)
                .toList();
        List<UUID> exhausted = stale.stream()
                .filter(claim -> claim.attemptCount() >= maxAttempts)
                .map(StaleClaim::id)
                .toList();

        repository.requeueStale(retryable, now, STALE_RECOVERY_ERROR, maxAttempts);
        repository.failStale(exhausted, now, STALE_EXHAUSTED_ERROR, maxAttempts);
        return stale.stream().map(StaleClaim::id).toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<UUID> claimDue(Instant now) {
        List<UUID> ids = repository.findDueIds(
                now, properties.getMaxAttempts(), properties.getBatchSize());
        repository.markProcessing(ids, now);
        return ids;
    }
}
