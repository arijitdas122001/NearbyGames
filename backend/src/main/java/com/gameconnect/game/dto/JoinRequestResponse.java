package com.gameconnect.game.dto;

import java.time.Instant;
import java.util.UUID;

import com.gameconnect.game.entity.JoinRequest.RequestStatus;

public record JoinRequestResponse(
        UUID id,
        UUID gameId,
        UUID userId,
        RequestStatus status,
        Instant createdAt,
        Instant decidedAt
) {
}