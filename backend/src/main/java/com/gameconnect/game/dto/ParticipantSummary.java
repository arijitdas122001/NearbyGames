package com.gameconnect.game.dto;

import java.util.UUID;

import com.gameconnect.game.entity.MatchParticipant.Role;

public record ParticipantSummary(
        UUID participantId,
        Role role,
        Boolean attended
) {
}