package com.gameconnect.game.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gameconnect.game.entity.MatchParticipant;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, UUID> {

    long countByGameId(UUID gameId);

    List<MatchParticipant> findByGameId(UUID gameId);
}