package com.gameconnect.profile.dto;

import java.time.Instant;
import java.util.UUID;

import com.gameconnect.auth.entity.User.Position;
import com.gameconnect.auth.entity.User.SkillLevel;

public record UserProfileResponse(
        UUID id,
        String displayName,
        String bio,
        String profileImageUrl,
        SkillLevel skillLevel,
        Position position,
        Instant createdAt,
        PlayerStatsResponse stats
) {
}