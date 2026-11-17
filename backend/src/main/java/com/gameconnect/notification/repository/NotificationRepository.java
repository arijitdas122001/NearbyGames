package com.gameconnect.notification.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gameconnect.notification.entity.Notification;
import com.gameconnect.notification.entity.Notification.NotificationStatus;
import com.gameconnect.notification.entity.Notification.NotificationType;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByGameIdAndUserIdAndNotificationType(
            UUID gameId, UUID userId, NotificationType notificationType);

    List<Notification> findByGameIdAndUserId(UUID gameId, UUID userId);

    List<Notification> findByStatus(NotificationStatus status);

    /**
     * Due notifications ordered oldest-first. The {@code (status, scheduled_at)}
     * index backs this query so future scheduled processing stays efficient.
     */
    List<Notification> findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            NotificationStatus status, Instant scheduledAt);
}
