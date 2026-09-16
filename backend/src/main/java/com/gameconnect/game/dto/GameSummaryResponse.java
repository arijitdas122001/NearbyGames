package com.gameconnect.game.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.gameconnect.auth.entity.User.SkillLevel;
import com.gameconnect.game.entity.Game.GameFormat;
import com.gameconnect.game.entity.Game.GameStatus;

public record GameSummaryResponse(
        UUID id,
        String turfName,
        String turfAddress,
        LocalDate gameDate,
        Instant startTime,
        Instant endTime,
        GameFormat format,
        SkillLevel skillLevel,
        int maximumPlayers,
        int currentPlayers,
        int spotsRemaining,
        Integer joiningFee,
        GameStatus status
) {
}