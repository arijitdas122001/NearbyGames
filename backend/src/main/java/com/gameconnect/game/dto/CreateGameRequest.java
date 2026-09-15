package com.gameconnect.game.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.gameconnect.auth.entity.User.SkillLevel;
import com.gameconnect.game.entity.Game.GameFormat;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateGameRequest(
        @NotBlank(message = "Turf name must not be blank")
        @Size(max = 120, message = "Turf name must be at most 120 characters")
        String turfName,

        @NotBlank(message = "Turf address must not be blank")
        @Size(max = 255, message = "Turf address must be at most 255 characters")
        String turfAddress,

        @NotNull(message = "Latitude must not be null")
        @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90", message = "Latitude must be between -90 and 90")
        BigDecimal latitude,

        @NotNull(message = "Longitude must not be null")
        @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180", message = "Longitude must be between -180 and 180")
        BigDecimal longitude,

        @NotNull(message = "Game date must not be null")
        LocalDate gameDate,

        @NotNull(message = "Start time must not be null")
        Instant startTime,

        @NotNull(message = "End time must not be null")
        Instant endTime,

        @NotNull(message = "Format must not be null")
        GameFormat format,

        @NotNull(message = "Skill level must not be null")
        SkillLevel skillLevel,

        @NotNull(message = "Maximum players must not be null")
        @Min(value = 5, message = "Maximum players must be between 5 and 22")
        @Max(value = 22, message = "Maximum players must be between 5 and 22")
        Integer maximumPlayers,

        @Min(value = 1, message = "Required players must be at least 1 when provided")
        Integer requiredPlayers,

        @Min(value = 0, message = "Joining fee must not be negative")
        Integer joiningFee,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description
) {
}