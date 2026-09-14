package com.gameconnect.profile.dto;

public record PlayerStatsResponse(
        int matchesPlayed,
        int matchesCompleted,
        Double attendanceRate,
        Double averageRating
) {
}