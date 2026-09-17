package com.gameconnect.game.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gameconnect.auth.entity.User;
import com.gameconnect.auth.repository.UserRepository;
import com.gameconnect.common.exception.BusinessException;
import com.gameconnect.game.dto.ApplicantSummary;
import com.gameconnect.game.dto.DecisionRequest.Decision;
import com.gameconnect.game.dto.JoinRequestDetailResponse;
import com.gameconnect.game.dto.JoinRequestResponse;
import com.gameconnect.game.entity.Game;
import com.gameconnect.game.entity.Game.GameStatus;
import com.gameconnect.game.entity.JoinRequest;
import com.gameconnect.game.entity.JoinRequest.RequestStatus;
import com.gameconnect.game.entity.MatchParticipant;
import com.gameconnect.game.repository.GameRepository;
import com.gameconnect.game.repository.JoinRequestRepository;
import com.gameconnect.game.repository.MatchParticipantRepository;
import com.gameconnect.profile.dto.PlayerStatsResponse;
import com.gameconnect.profile.repository.PlayerStatsRepository;
import com.gameconnect.security.JwtAuthenticationFilter.AuthenticatedUser;

@Service
public class JoinRequestService {

    private final JoinRequestRepository joinRequestRepository;
    private final GameRepository gameRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final UserRepository userRepository;
    private final PlayerStatsRepository playerStatsRepository;

    public JoinRequestService(JoinRequestRepository joinRequestRepository,
                              GameRepository gameRepository,
                              MatchParticipantRepository matchParticipantRepository,
                              UserRepository userRepository,
                              PlayerStatsRepository playerStatsRepository) {
        this.joinRequestRepository = joinRequestRepository;
        this.gameRepository = gameRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.userRepository = userRepository;
        this.playerStatsRepository = playerStatsRepository;
    }

    @Transactional
    public JoinRequestResponse createJoinRequest(AuthenticatedUser requester, UUID gameId) {
        userRepository.findById(requester.id())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "User not found"));

        Game game = findGame(gameId);

        if (game.getOwnerId().equals(requester.id())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "CANNOT_JOIN_OWN_GAME",
                    "You cannot request to join your own game");
        }

        if (game.getStatus() != GameStatus.OPEN) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "GAME_NOT_JOINABLE",
                    "This game is not open for new requests");
        }

        if (matchParticipantRepository.existsByGameIdAndUserId(gameId, requester.id())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "ALREADY_PARTICIPANT",
                    "You are already a participant in this game");
        }

        if (joinRequestRepository.existsByGameIdAndUserIdAndStatus(gameId, requester.id(), RequestStatus.PENDING)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "JOIN_REQUEST_EXISTS",
                    "You already have a pending request for this game");
        }

        long currentCount = matchParticipantRepository.countByGameId(gameId);
        if (currentCount >= game.getMaximumPlayers()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "GAME_FULL",
                    "This game is already full");
        }

        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setGameId(gameId);
        joinRequest.setUserId(requester.id());
        joinRequest.setStatus(RequestStatus.PENDING);
        joinRequest.setDecidedAt(null);

        try {
            return toResponse(joinRequestRepository.save(joinRequest));
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "JOIN_REQUEST_EXISTS",
                    "You already have a request for this game");
        }
    }

    @Transactional(readOnly = true)
    public JoinRequestResponse getMyRequest(AuthenticatedUser requester, UUID gameId) {
        findGame(gameId);
        return joinRequestRepository.findByGameIdAndUserId(gameId, requester.id())
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "REQUEST_NOT_FOUND",
                        "No join request found for this game"));
    }

    @Transactional(readOnly = true)
    public List<JoinRequestDetailResponse> getGameRequests(AuthenticatedUser owner, UUID gameId) {
        Game game = findGame(gameId);
        verifyOwner(game, owner);
        return joinRequestRepository.findByGameIdOrderByCreatedAtAsc(gameId).stream()
                .map(this::toDetail)
                .toList();
    }

    @Transactional
    public JoinRequestResponse decideRequest(AuthenticatedUser owner, UUID gameId,
                                             UUID requestId, Decision decision) {
        Game game = gameRepository.findByIdForUpdate(gameId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "GAME_NOT_FOUND",
                        "Game not found"));

        verifyOwner(game, owner);

        JoinRequest joinRequest = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "REQUEST_NOT_FOUND",
                        "Join request not found"));

        if (!joinRequest.getGameId().equals(gameId)) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "REQUEST_NOT_FOUND",
                    "Join request not found for this game");
        }

        if (joinRequest.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "REQUEST_ALREADY_DECIDED",
                    "This request has already been decided");
        }

        if (decision == Decision.REJECT) {
            joinRequest.setStatus(RequestStatus.REJECTED);
            joinRequest.setDecidedAt(Instant.now());
            joinRequestRepository.save(joinRequest);
            return toResponse(joinRequest);
        }

        return acceptUnderLock(game, joinRequest);
    }

    private JoinRequestResponse acceptUnderLock(Game game, JoinRequest joinRequest) {
        if (game.getStatus() != GameStatus.OPEN) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "GAME_NOT_JOINABLE",
                    "This game is not open for new players");
        }

        if (matchParticipantRepository.existsByGameIdAndUserId(game.getId(), joinRequest.getUserId())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "ALREADY_PARTICIPANT",
                    "Applicant is already a participant in this game");
        }

        long currentCount = matchParticipantRepository.countByGameId(game.getId());
        if (currentCount >= game.getMaximumPlayers()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "GAME_FULL",
                    "This game is already full");
        }

        MatchParticipant participant = new MatchParticipant();
        participant.setGameId(game.getId());
        participant.setUserId(joinRequest.getUserId());
        participant.setRole(MatchParticipant.Role.PLAYER);
        participant.setAttended(null);
        matchParticipantRepository.save(participant);

        joinRequest.setStatus(RequestStatus.ACCEPTED);
        joinRequest.setDecidedAt(Instant.now());

        if (currentCount + 1 == game.getMaximumPlayers()) {
            game.setStatus(GameStatus.FULL);
        }

        return toResponse(joinRequest);
    }

    private Game findGame(UUID gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "GAME_NOT_FOUND",
                        "Game not found"));
    }

    private void verifyOwner(Game game, AuthenticatedUser principal) {
        if (!game.getOwnerId().equals(principal.id())) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "Only the game owner can perform this action");
        }
    }

    private JoinRequestResponse toResponse(JoinRequest joinRequest) {
        return new JoinRequestResponse(
                joinRequest.getId(),
                joinRequest.getGameId(),
                joinRequest.getUserId(),
                joinRequest.getStatus(),
                joinRequest.getCreatedAt(),
                joinRequest.getDecidedAt());
    }

    private JoinRequestDetailResponse toDetail(JoinRequest joinRequest) {
        return new JoinRequestDetailResponse(
                joinRequest.getId(),
                joinRequest.getGameId(),
                joinRequest.getStatus(),
                joinRequest.getCreatedAt(),
                joinRequest.getDecidedAt(),
                toApplicantSummary(joinRequest.getUserId()));
    }

    private ApplicantSummary toApplicantSummary(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        PlayerStatsResponse stats = null;
        if (user != null) {
            PlayerStatsRepository.ParticipationStats participation =
                    playerStatsRepository.findParticipationStats(userId);
            Double averageRating = playerStatsRepository.findAverageRating(userId);
            stats = new PlayerStatsResponse(
                    participation.matchesPlayed(),
                    participation.matchesCompleted(),
                    participation.attendanceRate(),
                    averageRating);
        }
        return new ApplicantSummary(
                userId,
                user != null ? user.getDisplayName() : null,
                user != null ? user.getProfileImageUrl() : null,
                user != null ? user.getSkillLevel() : null,
                user != null ? user.getPosition() : null,
                stats);
    }
}