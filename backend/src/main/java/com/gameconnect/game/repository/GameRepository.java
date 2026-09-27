package com.gameconnect.game.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
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

    /**
     * Games the user is involved in, regardless of lifecycle stage. The owner
     * branch is a defensive fallback: {@code GameService.createGame} already
     * writes an {@code OWNER} match_participant row, so normally the
     * {@code EXISTS} subquery already matches.
     *
     * <p>The participant check is an {@code EXISTS} subquery rather than a
     * join, so the {@code game} row is never multiplied and an owner who is
     * also a participant can never produce a duplicate result.
     *
     * <p>Ordering is supplied via {@code pageable} so each category can use a
     * different sort; callers must always include the unique id as the final
     * tiebreaker to guarantee a deterministic total order. {@code statuses}
     * must never be null — pass the full set of statuses to express "no
     * lifecycle filter".
     */
    @Query("""
            SELECT g FROM Game g
            WHERE (g.ownerId = :userId
                   OR EXISTS (
                        SELECT mp.id FROM MatchParticipant mp
                        WHERE mp.gameId = g.id AND mp.userId = :userId
                   ))
              AND g.status IN :statuses
            """)
    Page<Game> findMyGames(@Param("userId") UUID userId,
                           @Param("statuses") Collection<GameStatus> statuses,
                           Pageable pageable);

    List<Game> findByStatusInAndEndTimeBefore(Collection<GameStatus> statuses, Instant endTimeBefore);
}