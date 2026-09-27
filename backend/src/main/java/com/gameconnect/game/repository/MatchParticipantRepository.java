package com.gameconnect.game.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gameconnect.game.entity.MatchParticipant;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, UUID> {

    long countByGameId(UUID gameId);

    boolean existsByGameIdAndUserId(UUID gameId, UUID userId);

    List<MatchParticipant> findByGameId(UUID gameId);

    List<MatchParticipant> findByGameIdOrderByJoinedAtAsc(UUID gameId);

    List<MatchParticipant> findByGameIdAndAttendedTrueOrderByJoinedAtAsc(UUID gameId);

    Optional<MatchParticipant> findByGameIdAndUserId(UUID gameId, UUID userId);

    Optional<MatchParticipant> findByIdAndGameId(UUID id, UUID gameId);

    /**
     * Batched lookup of one user's participation rows for a page of games.
     * Avoids an N+1 query when mapping a paged result set.
     */
    List<MatchParticipant> findByUserIdAndGameIdIn(UUID userId, Collection<UUID> gameIds);

    @Query("""
            SELECT mp.gameId AS gameId, COUNT(mp) AS participantCount
            FROM MatchParticipant mp
            WHERE mp.gameId IN :gameIds
            GROUP BY mp.gameId
            """)
    List<ParticipantCount> countByGameIds(@Param("gameIds") Collection<UUID> gameIds);

    interface ParticipantCount {
        UUID getGameId();

        long getParticipantCount();
    }
}