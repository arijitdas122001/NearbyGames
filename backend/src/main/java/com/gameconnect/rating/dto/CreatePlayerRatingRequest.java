package com.gameconnect.rating.dto;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreatePlayerRatingRequest(
        @NotNull(message = "Rated player must not be null")
        UUID ratedPlayerId,

        @NotNull(message = "Score must not be null")
        @Min(value = 1, message = "Score must be between 1 and 5")
        @Max(value = 5, message = "Score must be between 1 and 5")
        Integer score
) {
}
