package com.gameconnect.game.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gameconnect.auth.entity.User.SkillLevel;
import com.gameconnect.auth.repository.UserRepository;
import com.gameconnect.common.exception.BusinessException;
import com.gameconnect.game.dto.CreateGameRequest;
import com.gameconnect.game.dto.GameDetailResponse;
import com.gameconnect.game.dto.GameResponse;
import com.gameconnect.game.dto.GameSummaryResponse;
import com.gameconnect.game.dto.OwnerSummary;
import com.gameconnect.game.dto.PagedGamesResponse;
import com.gameconnect.game.entity.Game;
import com.gameconnect.game.entity.Game.GameFormat;
import com.gameconnect.game.entity.Game.GameStatus;
import com.gameconnect.game.entity.MatchParticipant;
import com.gameconnect.game.repository.GameRepository;
import com.gameconnect.game.repository.MatchParticipantRepository;
import com.gameconnect.security.JwtAuthenticationFilter.AuthenticatedUser;

@Service
public class GameService {

    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

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

    @Transactional(readOnly = true)
    public PagedGamesResponse listOpenGames(int page, int size,
                                            LocalDate date, String format,
                                            SkillLevel skillLevel,
                                            String q) {
        if (page < 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Page must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Size must be between 1 and " + MAX_PAGE_SIZE);
        }

        GameFormat parsedFormat = parseFormat(format);
        String pattern = toLikePattern(q);

        Page<Game> games = gameRepository.findDiscoveryGames(
                GameStatus.OPEN,
                Instant.now(),
                date,
                parsedFormat,
                skillLevel,
                pattern,
                PageRequest.of(page, size));

        Map<UUID, Long> participantCounts = countParticipants(games.getContent());

        List<GameSummaryResponse> content = games.getContent().stream()
                .map(game -> toSummary(game, participantCounts.getOrDefault(game.getId(), 0L)))
                .toList();

        return new PagedGamesResponse(
                content,
                games.getNumber(),
                games.getSize(),
                games.getTotalElements(),
                games.getTotalPages(),
                games.isFirst(),
                games.isLast());
    }

    @Transactional(readOnly = true)
    public GameDetailResponse getGameDetail(UUID gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "GAME_NOT_FOUND",
                        "Game not found"));

        OwnerSummary owner = userRepository.findById(game.getOwnerId())
                .map(u -> new OwnerSummary(
                        u.getId(),
                        u.getDisplayName(),
                        u.getProfileImageUrl(),
                        u.getSkillLevel()))
                .orElse(null);

        long currentPlayers = matchParticipantRepository.countByGameId(gameId);
        int spotsRemaining = Math.max(0, game.getMaximumPlayers() - (int) currentPlayers);

        return new GameDetailResponse(
                game.getId(),
                owner,
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
                (int) currentPlayers,
                spotsRemaining,
                game.getJoiningFee(),
                game.getDescription(),
                game.getStatus(),
                game.getCreatedAt());
    }

    private Map<UUID, Long> countParticipants(List<Game> games) {
        if (games.isEmpty()) {
            return Map.of();
        }
        List<UUID> gameIds = games.stream().map(Game::getId).toList();
        return matchParticipantRepository.countByGameIds(gameIds).stream()
                .collect(Collectors.toMap(
                        MatchParticipantRepository.ParticipantCount::getGameId,
                        MatchParticipantRepository.ParticipantCount::getParticipantCount));
    }

    private GameFormat parseFormat(String format) {
        if (format == null || format.isBlank()) {
            return null;
        }
        GameFormat parsed = GameFormat.fromDbValue(format);
        if (parsed == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Unknown game format: " + format);
        }
        return parsed;
    }

    private String toLikePattern(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private GameSummaryResponse toSummary(Game game, long currentPlayers) {
        int spotsRemaining = Math.max(0, game.getMaximumPlayers() - (int) currentPlayers);
        return new GameSummaryResponse(
                game.getId(),
                game.getTurfName(),
                game.getTurfAddress(),
                game.getGameDate(),
                game.getStartTime(),
                game.getEndTime(),
                game.getFormat(),
                game.getSkillLevel(),
                game.getMaximumPlayers(),
                (int) currentPlayers,
                spotsRemaining,
                game.getJoiningFee(),
                game.getStatus());
    }
}