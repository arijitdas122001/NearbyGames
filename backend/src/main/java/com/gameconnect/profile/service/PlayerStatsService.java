package com.gameconnect.profile.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gameconnect.profile.dto.PlayerStatsResponse;
import com.gameconnect.profile.repository.PlayerStatsRepository;

@Service
public class PlayerStatsService {

    private final PlayerStatsRepository playerStatsRepository;

    public PlayerStatsService(PlayerStatsRepository playerStatsRepository) {
        this.playerStatsRepository = playerStatsRepository;
    }

    @Transactional(readOnly = true)
    public PlayerStatsResponse getStats(UUID userId) {
        PlayerStatsRepository.ParticipationStats participation =
                playerStatsRepository.findParticipationStats(userId);
        PlayerStatsRepository.RatingStats rating = playerStatsRepository.findRatingStats(userId);
        return new PlayerStatsResponse(
                participation.matchesPlayed(),
                participation.matchesCompleted(),
                participation.attendanceRate(),
                rating.averageRating(),
                rating.ratingCount());
    }
}
