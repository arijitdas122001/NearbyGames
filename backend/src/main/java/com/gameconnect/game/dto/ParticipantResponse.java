package com.gameconnect.game.dto;

import java.time.Instant;
import java.util.UUID;

import com.gameconnect.auth.entity.User.SkillLevel;
import com.gameconnect.game.entity.MatchParticipant.Role;

public record ParticipantResponse(
        UUID participantId,
        UUID userId,
        String displayName,
        String profileImageUrl,
        SkillLevel skillLevel,
        Role role,
        Boolean attended,
        Instant joinedAt
) {
}