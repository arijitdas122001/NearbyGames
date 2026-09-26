package com.gameconnect.rating.dto;

import java.time.Instant;
import java.util.UUID;

public record PlayerRatingResponse(
        UUID id,
        UUID gameId,
        UUID ratedPlayerId,
        int score,
        Instant createdAt
) {
}
