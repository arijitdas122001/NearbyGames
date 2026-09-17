package com.gameconnect.game.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gameconnect.auth.entity.User.SkillLevel;
import com.gameconnect.game.entity.Game;
import com.gameconnect.game.entity.Game.GameFormat;
import com.gameconnect.game.entity.Game.GameStatus;

import jakarta.persistence.LockModeType;

public interface GameRepository extends JpaRepository<Game, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT g FROM Game g
            WHERE g.id = :id
            """)
    Optional<Game> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            SELECT g FROM Game g
            WHERE g.status = :status
              AND g.startTime >= :now
              AND g.gameDate = COALESCE(:date, g.gameDate)
              AND (:format IS NULL OR g.format = :format)
              AND (:skill IS NULL OR g.skillLevel = :skill)
              AND (:q IS NULL
                  OR LOWER(g.turfName) LIKE :q
                  OR LOWER(g.turfAddress) LIKE :q)
            ORDER BY g.startTime ASC
            """)
    Page<Game> findDiscoveryGames(@Param("status") GameStatus status,
                                  @Param("now") Instant now,
                                  @Param("date") LocalDate date,
                                  @Param("format") GameFormat format,
                                  @Param("skill") SkillLevel skill,
                                  @Param("q") String q,
                                  Pageable pageable);
}