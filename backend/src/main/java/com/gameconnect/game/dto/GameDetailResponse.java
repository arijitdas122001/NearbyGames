package com.gameconnect.game.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.gameconnect.auth.entity.User.SkillLevel;
import com.gameconnect.game.entity.Game.GameFormat;
import com.gameconnect.game.entity.Game.GameStatus;

public record GameDetailResponse(
        UUID id,
        OwnerSummary owner,
        String turfName,
        String turfAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDate gameDate,
        Instant startTime,
        Instant endTime,
        GameFormat format,
        SkillLevel skillLevel,
        int maximumPlayers,
        Integer requiredPlayers,
        int currentPlayers,
        int spotsRemaining,
        Integer joiningFee,
        String description,
        GameStatus status,
        Instant createdAt
) {
}