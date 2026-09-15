package com.gameconnect.game.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gameconnect.auth.repository.UserRepository;
import com.gameconnect.common.exception.BusinessException;
import com.gameconnect.game.dto.CreateGameRequest;
import com.gameconnect.game.dto.GameResponse;
import com.gameconnect.game.entity.Game;
import com.gameconnect.game.entity.Game.GameStatus;
import com.gameconnect.game.entity.MatchParticipant;
import com.gameconnect.game.repository.GameRepository;
import com.gameconnect.game.repository.MatchParticipantRepository;
import com.gameconnect.security.JwtAuthenticationFilter.AuthenticatedUser;

@Service
public class GameService {

    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

    private final GameRepository gameRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final UserRepository userRepository;

    public GameService(GameRepository gameRepository,
                       MatchParticipantRepository matchParticipantRepository,
                       UserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GameResponse createGame(AuthenticatedUser owner, CreateGameRequest request) {
        userRepository.findById(owner.id())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "User not found"));

        validateScheduling(request);

        Game game = new Game();
        game.setOwnerId(owner.id());
        game.setTurfName(request.turfName().trim());
        game.setTurfAddress(request.turfAddress().trim());
        game.setLatitude(request.latitude());
        game.setLongitude(request.longitude());
        game.setGameDate(request.gameDate());
        game.setStartTime(request.startTime());
        game.setEndTime(request.endTime());
        game.setFormat(request.format());
        game.setSkillLevel(request.skillLevel());
        game.setMaximumPlayers(request.maximumPlayers());
        game.setRequiredPlayers(request.requiredPlayers());
        game.setJoiningFee(request.joiningFee());
        game.setDescription(request.description());
        game.setStatus(GameStatus.OPEN);

        validateConfiguration(game);

        gameRepository.save(game);

        MatchParticipant participant = new MatchParticipant();
        participant.setGameId(game.getId());
        participant.setUserId(owner.id());
        participant.setRole(MatchParticipant.Role.OWNER);
        participant.setAttended(null);
        matchParticipantRepository.save(participant);

        return toResponse(game);
    }

    private void validateScheduling(CreateGameRequest request) {
        if (request.gameDate().isBefore(LocalDate.now(INDIA_ZONE))) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Game date must not be in the past");
        }
        if (!request.startTime().isAfter(Instant.now())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Start time must be in the future");
        }
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "End time must be after start time");
        }
    }

    private void validateConfiguration(Game game) {
        if (game.getRequiredPlayers() != null
                && game.getRequiredPlayers() > game.getMaximumPlayers()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Required players must not exceed maximum players");
        }
    }

    private GameResponse toResponse(Game game) {
        return new GameResponse(
                game.getId(),
                game.getOwnerId(),
                game.getTurfName(),
                game.getTurfAddress(),
                game.getLatitude(),
                game.getLongitude(),
                game.getGameDate(),
                game.getStartTime(),
                game.getEndTime(),
                game.getFormat(),
                game.getSkillLevel(),
                game.getMaximumPlayers(),
                game.getRequiredPlayers(),
                game.getJoiningFee(),
                game.getDescription(),
                game.getStatus(),
                game.getCreatedAt());
    }
}