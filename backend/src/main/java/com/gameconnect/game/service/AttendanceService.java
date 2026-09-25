package com.gameconnect.game.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gameconnect.auth.entity.User;
import com.gameconnect.auth.repository.UserRepository;
import com.gameconnect.common.exception.BusinessException;
import com.gameconnect.game.dto.ParticipantResponse;
import com.gameconnect.game.entity.Game;
import com.gameconnect.game.entity.Game.GameStatus;
import com.gameconnect.game.entity.MatchParticipant;
import com.gameconnect.game.repository.GameRepository;
import com.gameconnect.game.repository.MatchParticipantRepository;
import com.gameconnect.security.JwtAuthenticationFilter.AuthenticatedUser;

@Service
public class AttendanceService {

    private final GameRepository gameRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final UserRepository userRepository;
    private final Duration attendanceGrace;

    public AttendanceService(GameRepository gameRepository,
                             MatchParticipantRepository matchParticipantRepository,
                             UserRepository userRepository,
                             @Value("${game-completion.grace-period:6h}") Duration attendanceGrace) {
        this.gameRepository = gameRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.userRepository = userRepository;
        this.attendanceGrace = attendanceGrace;
    }

    public Duration getAttendanceGrace() {
        return attendanceGrace;
    }

    @Transactional(readOnly = true)
    public List<ParticipantResponse> getParticipants(AuthenticatedUser owner, UUID gameId) {
        Game game = findGame(gameId);
        verifyOwner(game, owner);
        return matchParticipantRepository.findByGameIdOrderByJoinedAtAsc(gameId).stream()
                .map(this::toParticipantResponse)
                .toList();
    }

    @Transactional
    public ParticipantResponse updateAttendance(AuthenticatedUser owner, UUID gameId,
                                                UUID participantId, boolean attended) {
        Game game = gameRepository.findByIdForUpdate(gameId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "GAME_NOT_FOUND",
                        "Game not found"));

        verifyOwner(game, owner);
        assertAttendable(game);

        MatchParticipant participant = matchParticipantRepository
                .findByIdAndGameId(participantId, gameId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "PARTICIPANT_NOT_FOUND",
                        "Participant not found for this game"));

        participant.setAttended(attended);
        matchParticipantRepository.save(participant);

        return toParticipantResponse(participant);
    }

    @Transactional
    public void completeGame(UUID gameId) {
        Game game = gameRepository.findByIdForUpdate(gameId).orElse(null);
        if (game == null) {
            return;
        }
        if (game.getStatus() == GameStatus.COMPLETED || game.getStatus() == GameStatus.CANCELLED) {
            return;
        }
        Instant now = Instant.now();
        if (now.isBefore(game.getEndTime().plus(attendanceGrace))) {
            return;
        }
        game.setStatus(GameStatus.COMPLETED);
    }

    private void assertAttendable(Game game) {
        if (game.getStatus() == GameStatus.COMPLETED) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "GAME_ALREADY_COMPLETED",
                    "Attendance can no longer be updated because the game is completed");
        }
        if (game.getStatus() != GameStatus.OPEN
                && game.getStatus() != GameStatus.FULL
                && game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "GAME_NOT_ACTIVE",
                    "Attendance cannot be updated for a cancelled game");
        }

        Instant now = Instant.now();
        if (now.isBefore(game.getStartTime())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "GAME_NOT_STARTED",
                    "Attendance can only be marked after the game starts");
        }
        if (now.isAfter(game.getEndTime().plus(attendanceGrace))) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "GAME_ALREADY_COMPLETED",
                    "The attendance window for this game has closed");
        }
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

    private ParticipantResponse toParticipantResponse(MatchParticipant participant) {
        User user = userRepository.findById(participant.getUserId()).orElse(null);
        return new ParticipantResponse(
                participant.getId(),
                participant.getUserId(),
                user != null ? user.getDisplayName() : null,
                user != null ? user.getProfileImageUrl() : null,
                user != null ? user.getSkillLevel() : null,
                participant.getRole(),
                participant.getAttended(),
                participant.getJoinedAt());
    }
}