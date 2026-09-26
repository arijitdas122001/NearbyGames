package com.gameconnect.rating.dto;

import java.util.List;
import java.util.UUID;

public record GameRatingsResponse(
        UUID gameId,
        List<RateablePlayerResponse> players
) {
}
