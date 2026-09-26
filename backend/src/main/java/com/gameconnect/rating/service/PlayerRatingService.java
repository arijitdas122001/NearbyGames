package com.gameconnect.rating.service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gameconnect.auth.entity.User;
import com.gameconnect.auth.repository.UserRepository;
import com.gameconnect.common.exception.BusinessException;
import com.gameconnect.game.entity.Game;
import com.gameconnect.game.entity.Game.GameStatus;
import com.gameconnect.game.entity.MatchParticipant;
import com.gameconnect.game.repository.GameRepository;
import com.gameconnect.game.repository.MatchParticipantRepository;
import com.gameconnect.rating.dto.CreatePlayerRatingRequest;
import com.gameconnect.rating.dto.GameRatingsResponse;
import com.gameconnect.rating.dto.PlayerRatingResponse;
import com.gameconnect.rating.dto.RateablePlayerResponse;
import com.gameconnect.rating.entity.PlayerRating;
import com.gameconnect.rating.repository.PlayerRatingRepository;
import com.gameconnect.security.JwtAuthenticationFilter.AuthenticatedUser;

@Service
public class PlayerRatingService {

    private final GameRepository gameRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final UserRepository userRepository;

    public PlayerRatingService(GameRepository gameRepository,
                               MatchParticipantRepository matchParticipantRepository,
                               PlayerRatingRepository playerRatingRepository,
                               UserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.playerRatingRepository = playerRatingRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public GameRatingsResponse getEligibleRatings(AuthenticatedUser principal, UUID gameId) {
        Game game = findGame(gameId);
        assertCompleted(game);
        requireEligibleRater(gameId, principal.id());

        Map<UUID, Integer> myRatings = new HashMap<>();
        for (PlayerRating rating : playerRatingRepository.findByGameIdAndRaterId(gameId, principal.id())) {
            myRatings.put(rating.getRateeId(), (int) rating.getScore());
        }

        List<MatchParticipant> participants = matchParticipantRepository
                .findByGameIdAndAttendedTrueOrderByJoinedAtAsc(gameId);
        Map<UUID, User> users = userRepository.findAllById(
                        participants.stream().map(MatchParticipant::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<RateablePlayerResponse> players = participants.stream()
                .map(MatchParticipant::getUserId)
                .filter(userId -> !userId.equals(principal.id()))
                .map(users::get)
                .filter(user -> user != null)
                .map(user -> new RateablePlayerResponse(
                        user.getId(),
                        user.getDisplayName(),
                        user.getProfileImageUrl(),
                        myRatings.get(user.getId())))
                .toList();

        return new GameRatingsResponse(gameId, players);
    }

    @Transactional
    public PlayerRatingResponse createRating(AuthenticatedUser principal,
                                             UUID gameId,
                                             CreatePlayerRatingRequest request) {
        Game game = findGame(gameId);
        assertCompleted(game);
        requireEligibleRater(gameId, principal.id());

        if (principal.id().equals(request.ratedPlayerId())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "SELF_RATING_NOT_ALLOWED",
                    "You cannot rate yourself");
        }

        userRepository.findById(request.ratedPlayerId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "User not found"));

        MatchParticipant ratedPlayer = matchParticipantRepository
                .findByGameIdAndUserId(gameId, request.ratedPlayerId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.FORBIDDEN,
                        "RATED_PLAYER_NOT_PARTICIPANT",
                        "The rated player did not participate in this game"));
        requireAttended(ratedPlayer, "RATED_PLAYER_ATTENDANCE_REQUIRED",
                "The rated player did not attend this game");

        if (playerRatingRepository.existsByGameIdAndRaterIdAndRateeId(
                gameId, principal.id(), request.ratedPlayerId())) {
            throw duplicateRating();
        }

        PlayerRating rating = new PlayerRating(
                gameId,
                principal.id(),
                request.ratedPlayerId(),
                request.score());
        try {
            PlayerRating saved = playerRatingRepository.saveAndFlush(rating);
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueRatingViolation(ex)) {
                throw duplicateRating();
            }
            throw ex;
        }
    }

    private Game findGame(UUID gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "GAME_NOT_FOUND",
                        "Game not found"));
    }

    private void assertCompleted(Game game) {
        if (game.getStatus() != GameStatus.COMPLETED) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "GAME_NOT_COMPLETED",
                    "Ratings can only be submitted for completed games");
        }
    }

    private MatchParticipant requireEligibleRater(UUID gameId, UUID userId) {
        MatchParticipant participant = matchParticipantRepository
                .findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.FORBIDDEN,
                        "RATER_NOT_PARTICIPANT",
                        "You did not participate in this game"));
        requireAttended(participant, "RATER_ATTENDANCE_REQUIRED",
                "You must attend this game before rating players");
        return participant;
    }

    private void requireAttended(MatchParticipant participant, String code, String message) {
        if (!Boolean.TRUE.equals(participant.getAttended())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, code, message);
        }
    }

    private BusinessException duplicateRating() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "RATING_ALREADY_EXISTS",
                "You already rated this player for this game");
    }

    private boolean isUniqueRatingViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("uq_rating")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private PlayerRatingResponse toResponse(PlayerRating rating) {
        return new PlayerRatingResponse(
                rating.getId(),
                rating.getGameId(),
                rating.getRateeId(),
                (int) rating.getScore(),
                rating.getCreatedAt());
    }
}
