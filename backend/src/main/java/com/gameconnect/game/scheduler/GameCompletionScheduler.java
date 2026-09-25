package com.gameconnect.game.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.gameconnect.game.entity.Game;
import com.gameconnect.game.entity.Game.GameStatus;
import com.gameconnect.game.repository.GameRepository;
import com.gameconnect.game.service.AttendanceService;

@Component
public class GameCompletionScheduler {

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