package com.gameconnect.game.dto;

import jakarta.validation.constraints.NotNull;

public record AttendanceRequest(
        @NotNull(message = "Attended must not be null")
        Boolean attended
) {
}