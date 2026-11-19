package com.gameconnect.notification.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationClaimRepository {

    public record DeliveryTarget(UUID notificationId, UUID gameId, String email,
                                 String turfName, String turfAddress,
                                 LocalDate gameDate, Instant startTime, String format) {
    }

    public enum FailureResult {
        RETRIED, FAILED, NOT_FOUND
    }

    public record StaleClaim(UUID id, int attemptCount) {
    }

    private final JdbcTemplate jdbcTemplate;

    public NotificationClaimRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UUID> findDueIds(Instant now, int maxAttempts, int batchSize) {
        String sql = """
                SELECT n.id
                FROM notification n
                WHERE n.notification_type = 'GAME_REMINDER'
                  AND n.status = 'PENDING'
                  AND n.scheduled_at <= ?
                  AND n.attempt_count < ?
                ORDER BY n.scheduled_at, n.id
                LIMIT ?
                FOR UPDATE OF n SKIP LOCKED
                """;
        return queryIds(sql, timestamp(now), maxAttempts, batchSize);
    }

    public List<StaleClaim> findStaleClaims(Instant staleBefore, int batchSize) {
        String sql = """
                SELECT n.id, n.attempt_count
                FROM notification n
                WHERE n.notification_type = 'GAME_REMINDER'
                  AND n.status = 'PROCESSING'
                  AND n.updated_at < ?
                ORDER BY n.updated_at, n.id
                LIMIT ?
                FOR UPDATE OF n SKIP LOCKED
                """;
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new StaleClaim(
                        (UUID) rs.getObject("id"), rs.getInt("attempt_count")),
                timestamp(staleBefore), batchSize);
    }

    public void markProcessing(List<UUID> ids, Instant now) {
        if (ids.isEmpty()) {
            return;
        }
        Object[] args = new Object[ids.size() + 1];
        args[0] = timestamp(now);
        for (int i = 0; i < ids.size(); i++) {
            args[i + 1] = ids.get(i);
        }
        String sql = """
                UPDATE notification
                SET status = 'PROCESSING', attempt_count = attempt_count + 1, updated_at = ?
                WHERE id IN (%s) AND status = 'PENDING'
                """.formatted(placeholders(ids.size()));
        jdbcTemplate.update(sql, args);
    }

    public void requeueStale(List<UUID> ids, Instant now, String error, int maxAttempts) {
        if (ids.isEmpty()) {
            return;
        }
        Object[] args = new Object[ids.size() + 4];
        args[0] = timestamp(now);
        args[1] = timestamp(now);
        args[2] = error;
        for (int i = 0; i < ids.size(); i++) {
            args[i + 3] = ids.get(i);
        }
        args[ids.size() + 3] = maxAttempts;
        String sql = """
                UPDATE notification
                SET status = 'PENDING', scheduled_at = ?, updated_at = ?, last_error = ?
                WHERE id IN (%s)
                  AND status = 'PROCESSING'
                  AND attempt_count < ?
                """.formatted(placeholders(ids.size()));
        jdbcTemplate.update(sql, args);
    }

    public void failStale(List<UUID> ids, Instant now, String error, int maxAttempts) {
        if (ids.isEmpty()) {
            return;
        }
        Object[] args = new Object[ids.size() + 3];
        args[0] = timestamp(now);
        args[1] = error;
        for (int i = 0; i < ids.size(); i++) {
            args[i + 2] = ids.get(i);
        }
        args[ids.size() + 2] = maxAttempts;
        String sql = """
                UPDATE notification
                SET status = 'FAILED', updated_at = ?, last_error = ?
                WHERE id IN (%s)
                  AND status = 'PROCESSING'
                  AND attempt_count >= ?
                """.formatted(placeholders(ids.size()));
        jdbcTemplate.update(sql, args);
    }

    public Optional<DeliveryTarget> findDeliveryTarget(UUID notificationId) {
        String sql = """
                SELECT n.id, g.id AS game_id, u.email, g.turf_name, g.turf_address,
                       g.game_date, g.start_time, g.format
                FROM notification n
                JOIN game g ON g.id = n.game_id
                JOIN app_user u ON u.id = n.user_id
                WHERE n.id = ?
                  AND n.notification_type = 'GAME_REMINDER'
                  AND n.status = 'PROCESSING'
                """;
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(new DeliveryTarget(
                    (UUID) rs.getObject("id"),
                    (UUID) rs.getObject("game_id"),
                    rs.getString("email"),
                    rs.getString("turf_name"),
                    rs.getString("turf_address"),
                    readLocalDate(rs, "game_date"),
                    rs.getTimestamp("start_time").toInstant(),
                    rs.getString("format")));
        }, notificationId);
    }

    public int attemptCount(UUID notificationId) {
        Integer count = jdbcTemplate.query(
                "SELECT attempt_count FROM notification WHERE id = ?",
                rs -> rs.next() ? rs.getInt("attempt_count") : null, notificationId);
        return count == null ? 0 : count;
    }

    public void markSent(UUID notificationId, String providerMessageId, Instant now) {
        jdbcTemplate.update("""
                UPDATE notification
                SET status = 'SENT', sent_at = ?, provider_message_id = ?, last_error = NULL,
                    updated_at = ?
                WHERE id = ? AND status = 'PROCESSING'
                """, timestamp(now), providerMessageId, timestamp(now), notificationId);
    }

    public FailureResult finalizeFailure(UUID notificationId, String error, boolean retryable,
                                         Instant nextAttemptAt, int maxAttempts, Instant now) {
        Integer attempts = jdbcTemplate.query(
                "SELECT attempt_count FROM notification WHERE id = ? FOR UPDATE",
                rs -> rs.next() ? rs.getInt("attempt_count") : null, notificationId);
        if (attempts == null) {
            return FailureResult.NOT_FOUND;
        }

        if (!retryable || attempts >= maxAttempts) {
            jdbcTemplate.update("""
                    UPDATE notification
                    SET status = 'FAILED', last_error = ?, updated_at = ?
                    WHERE id = ? AND status = 'PROCESSING'
                    """, error, timestamp(now), notificationId);
            return FailureResult.FAILED;
        }

        jdbcTemplate.update("""
                UPDATE notification
                SET status = 'PENDING', scheduled_at = ?, last_error = ?, updated_at = ?
                WHERE id = ? AND status = 'PROCESSING'
                """, timestamp(nextAttemptAt), error, timestamp(now), notificationId);
        return FailureResult.RETRIED;
    }

    private List<UUID> queryIds(String sql, Object... args) {
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> (UUID) rs.getObject("id"), args);
    }

    private LocalDate readLocalDate(ResultSet rs, String column) throws SQLException {
        java.sql.Date date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }

    private String placeholders(int size) {
        return String.join(",", Collections.nCopies(size, "?"));
    }
}
