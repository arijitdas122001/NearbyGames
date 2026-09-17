package com.gameconnect.game.dto;

import jakarta.validation.constraints.NotNull;

public record DecisionRequest(
        @NotNull(message = "Action must not be null")
        Decision action
) {
    public enum Decision {
        ACCEPT, REJECT
    }
}