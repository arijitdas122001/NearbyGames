package com.gameconnect.game.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gameconnect.game.entity.Game;
import com.gameconnect.game.entity.Game.GameStatus;
import com.gameconnect.game.repository.GameRepository;
import com.gameconnect.game.service.AttendanceService;

@Component
public class GameCompletionScheduler {

    private static final List<GameStatus> STARTABLE = List.of(GameStatus.OPEN, GameStatus.FULL);

    private final GameRepository gameRepository;
    private final AttendanceService attendanceService;
    private final Duration attendanceGrace;

    public GameCompletionScheduler(GameRepository gameRepository,
                                   AttendanceService attendanceService,
                                   @Value("${game-completion.grace-period:6h}") Duration attendanceGrace) {
        this.gameRepository = gameRepository;
        this.attendanceService = attendanceService;
        this.attendanceGrace = attendanceGrace;
    }

    /**
     * Flips OPEN/FULL games to IN_PROGRESS once their start time has passed.
     * The game row is re-read under a pessimistic write lock and re-validated
     * so a concurrent accept/completion decision is never overwritten.
     *
     * <p>Join and accept paths require {@code status == OPEN}, so this is what
     * stops new players being admitted after kick-off. Granularity is the
     * scheduler tick (60 s): a join can still slip through in the moments
     * before the tick runs.
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 15_000)
    @Transactional
    public void startDueGames() {
        Instant now = Instant.now();
        List<Game> dueGames = gameRepository.findByStatusInAndStartTimeBefore(STARTABLE, now);
        for (Game game : dueGames) {
            Game locked = gameRepository.findByIdForUpdate(game.getId()).orElse(null);
            if (locked == null) {
                continue;
            }
            GameStatus status = locked.getStatus();
            if ((status == GameStatus.OPEN || status == GameStatus.FULL)
                    && !locked.getStartTime().isAfter(now)) {
                locked.setStatus(GameStatus.IN_PROGRESS);
            }
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void completeDueGames() {
        Instant cutoff = Instant.now().minus(attendanceGrace);
        List<Game> dueGames = gameRepository.findByStatusInAndEndTimeBefore(
                List.of(GameStatus.OPEN, GameStatus.FULL, GameStatus.IN_PROGRESS),
                cutoff);
        for (Game game : dueGames) {
            attendanceService.completeGame(game.getId());
        }
    }
}