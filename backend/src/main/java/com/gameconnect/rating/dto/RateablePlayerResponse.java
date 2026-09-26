package com.gameconnect.rating.dto;

import java.util.UUID;

public record RateablePlayerResponse(
        UUID userId,
        String displayName,
        String profileImageUrl,
        Integer myRating
) {
}
