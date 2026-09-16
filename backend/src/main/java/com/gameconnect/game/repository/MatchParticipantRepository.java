package com.gameconnect.game.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gameconnect.game.entity.MatchParticipant;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, UUID> {

    long countByGameId(UUID gameId);

    List<MatchParticipant> findByGameId(UUID gameId);

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