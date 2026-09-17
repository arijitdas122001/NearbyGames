package com.gameconnect.game.dto;

import java.time.Instant;
import java.util.UUID;

import com.gameconnect.game.entity.JoinRequest.RequestStatus;

public record JoinRequestDetailResponse(
        UUID id,
        UUID gameId,
        RequestStatus status,
        Instant createdAt,
        Instant decidedAt,
        ApplicantSummary applicant
) {
}