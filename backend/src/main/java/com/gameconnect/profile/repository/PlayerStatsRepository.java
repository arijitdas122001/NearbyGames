package com.gameconnect.profile.repository;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Computes player statistics from the match_participant and player_rating
 * tables using native SQL. Per PRODUCT.md, only COMPLETED games contribute to
 * match statistics.
 */
@Repository
public class PlayerStatsRepository {

    private final JdbcTemplate jdbcTemplate;

    public PlayerStatsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ParticipationStats findParticipationStats(UUID userId) {
        return jdbcTemplate.queryForObject("""
                SELECT
                    COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' THEN mp.game_id END) AS matches_played,
                    COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' THEN mp.game_id END) AS matches_completed,
                    CASE
                        WHEN COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' THEN mp.game_id END) = 0 THEN NULL
                        ELSE CAST(
                            COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' AND mp.attended = TRUE THEN mp.game_id END)
                            AS DOUBLE PRECISION
                        ) / COUNT(DISTINCT CASE WHEN g.status = 'COMPLETED' THEN mp.game_id END)
                    END AS attendance_rate
                FROM match_participant mp
                JOIN game g ON g.id = mp.game_id
                WHERE mp.user_id = ?
                """,
                (rs, rowNum) -> new ParticipationStats(
                        rs.getInt("matches_played"),
                        rs.getInt("matches_completed"),
                        rs.getObject("attendance_rate", Double.class)),
                userId);
    }

    public Double findAverageRating(UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT AVG(pr.score) FROM player_rating pr WHERE pr.ratee_id = ?",
                Double.class,
                userId);
    }

    public record ParticipationStats(int matchesPlayed, int matchesCompleted, Double attendanceRate) {
    }
}