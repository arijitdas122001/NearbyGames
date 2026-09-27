package com.gameconnect.game.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gameconnect.common.exception.BusinessException;
import com.gameconnect.game.dto.MyGameCategory;
import com.gameconnect.game.dto.MyGameSummaryResponse;
import com.gameconnect.game.dto.PagedMyGamesResponse;
import com.gameconnect.game.entity.Game;
import com.gameconnect.game.entity.Game.GameStatus;
import com.gameconnect.game.entity.MatchParticipant;
import com.gameconnect.game.entity.MatchParticipant.Role;
import com.gameconnect.game.repository.GameRepository;
import com.gameconnect.game.repository.MatchParticipantRepository;
import com.gameconnect.security.JwtAuthenticationFilter.AuthenticatedUser;

/**
 * "My Games": the games the authenticated user is involved in, across the whole
 * lifecycle. Deliberately separate from
 * {@link com.gameconnect.game.service.GameService#listOpenGames}, which answers
 * a different question — "what can I join?".
 */
@Service
public class MyGamesService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final GameRepository gameRepository;
    private final MatchParticipantRepository matchParticipantRepository;

    public MyGamesService(GameRepository gameRepository,
                          MatchParticipantRepository matchParticipantRepository) {
        this.gameRepository = gameRepository;
        this.matchParticipantRepository = matchParticipantRepository;
    }

    @Transactional(readOnly = true)
    public PagedMyGamesResponse listMyGames(AuthenticatedUser principal,
                                            int page, int size,
                                            MyGameCategory category) {
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

        MyGameCategory resolved = category == null ? MyGameCategory.ALL : category;

        // Never pass a null collection: Hibernate cannot expand "IN :param" for
        // null. ALL carries every GameStatus, so it is equivalent to no filter.
        Collection<GameStatus> statuses = resolved.statuses();

        Page<Game> games = gameRepository.findMyGames(
                principal.id(),
                statuses,
                PageRequest.of(page, size, sortFor(resolved)));

        Map<UUID, Long> participantCounts = countParticipants(games.getContent());
        Map<UUID, MatchParticipant> myParticipations =
                myParticipations(principal.id(), games.getContent());

        List<MyGameSummaryResponse> content = games.getContent().stream()
                .map(game -> toResponse(game,
                        participantCounts.getOrDefault(game.getId(), 0L),
                        myParticipations.get(game.getId())))
                .toList();

        return new PagedMyGamesResponse(
                content,
                games.getNumber(),
                games.getSize(),
                games.getTotalElements(),
                games.getTotalPages(),
                games.isFirst(),
                games.isLast());
    }

    /**
     * Every ordering is a total order: the unique id is always the final
     * tiebreaker, so repeated calls can never differ.
     */
    private Sort sortFor(MyGameCategory category) {
        return switch (category) {
            case UPCOMING, IN_PROGRESS ->
                    Sort.by(Sort.Order.asc("startTime"), Sort.Order.asc("id"));
            case COMPLETED, CANCELLED, ALL ->
                    Sort.by(Sort.Order.desc("startTime"), Sort.Order.asc("id"));
        };
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

    private Map<UUID, MatchParticipant> myParticipations(UUID userId, List<Game> games) {
        if (games.isEmpty()) {
            return Map.of();
        }
        List<UUID> gameIds = games.stream().map(Game::getId).toList();
        return matchParticipantRepository.findByUserIdAndGameIdIn(userId, gameIds).stream()
                .collect(Collectors.toMap(MatchParticipant::getGameId, p -> p));
    }

    private MyGameSummaryResponse toResponse(Game game, long currentPlayers,
                                             MatchParticipant myParticipation) {
        int spotsRemaining = Math.max(0, game.getMaximumPlayers() - (int) currentPlayers);
        Role myRole = myParticipation != null
                ? myParticipation.getRole()
                : Role.OWNER;
        Boolean myAttended = myParticipation != null ? myParticipation.getAttended() : null;
        boolean canRate = game.getStatus() == GameStatus.COMPLETED
                && Boolean.TRUE.equals(myAttended);

        return new MyGameSummaryResponse(
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
                game.getStatus(),
                MyGameCategory.fromGameStatus(game.getStatus()),
                myRole,
                myAttended,
                canRate);
    }
}
