package com.gameconnect.notification.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gameconnect.notification.entity.Notification;
import com.gameconnect.notification.entity.Notification.NotificationStatus;
import com.gameconnect.notification.entity.Notification.NotificationType;
import com.gameconnect.notification.repository.NotificationRepository;

@Service
public class NotificationSchedulingService {

    private static final Duration REMINDER_LEAD_TIME = Duration.ofHours(1);

    private final NotificationRepository notificationRepository;

    public NotificationSchedulingService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void scheduleAcceptedParticipant(UUID gameId, UUID userId, Instant gameStartTime) {
        Instant scheduledAt = reminderTime(gameStartTime);
        Notification notification = notificationRepository
                .findByGameIdAndUserIdAndNotificationType(gameId, userId, NotificationType.GAME_REMINDER)
                .orElse(null);

        if (notification == null) {
            Notification created = new Notification();
            created.setGameId(gameId);
            created.setUserId(userId);
            created.setNotificationType(NotificationType.GAME_REMINDER);
            created.setScheduledAt(scheduledAt);
            created.setStatus(NotificationStatus.PENDING);
            notificationRepository.save(created);
            return;
        }

        if (notification.getStatus() == NotificationStatus.PENDING
                || notification.getStatus() == NotificationStatus.CANCELLED) {
            notification.setStatus(NotificationStatus.PENDING);
            notification.setScheduledAt(scheduledAt);
            notification.setLastError(null);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void cancelPendingForParticipant(UUID gameId, UUID userId) {
        notificationRepository
                .findByGameIdAndUserIdAndNotificationType(gameId, userId, NotificationType.GAME_REMINDER)
                .filter(notification -> notification.getStatus() == NotificationStatus.PENDING)
                .ifPresent(notification -> {
                    notification.setStatus(NotificationStatus.CANCELLED);
                    notification.setLastError(null);
                    notificationRepository.save(notification);
                });
    }

    @Transactional
    public void cancelAllPendingForGame(UUID gameId) {
        cancelOrReschedule(gameId, null);
    }

    @Transactional
    public void reschedulePendingForGame(UUID gameId, Instant newStartTime) {
        cancelOrReschedule(gameId, reminderTime(newStartTime));
    }

    private void cancelOrReschedule(UUID gameId, Instant newScheduledAt) {
        List<Notification> pending = notificationRepository
                .findByGameIdAndNotificationType(gameId, NotificationType.GAME_REMINDER).stream()
                .filter(notification -> notification.getStatus() == NotificationStatus.PENDING)
                .toList();

        for (Notification notification : pending) {
            if (newScheduledAt == null) {
                notification.setStatus(NotificationStatus.CANCELLED);
                notification.setLastError(null);
            } else {
                notification.setScheduledAt(newScheduledAt);
            }
            notificationRepository.save(notification);
        }
    }

    private Instant reminderTime(Instant gameStartTime) {
        Instant reminder = gameStartTime.minus(REMINDER_LEAD_TIME);
        return reminder.isAfter(Instant.now()) ? reminder : Instant.now();
    }
}
