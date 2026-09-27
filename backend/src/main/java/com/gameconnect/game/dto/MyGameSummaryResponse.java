package com.gameconnect.game.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.gameconnect.auth.entity.User.SkillLevel;
import com.gameconnect.game.entity.Game.GameFormat;
import com.gameconnect.game.entity.Game.GameStatus;
import com.gameconnect.game.entity.MatchParticipant.Role;

/**
 * A game the authenticated user is involved in, for the "My Games" surface.
 *
 * <p>Flat superset of {@link GameSummaryResponse} plus the viewer's own
 * participation context, so the frontend never needs a second request to learn
 * whether the current user is the host, a player, or eligible to rate.
 */
public record MyGameSummaryResponse(
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
        GameStatus status,
        MyGameCategory category,
        Role myRole,
        Boolean myAttended,
        boolean canRate
) {
}
