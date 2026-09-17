package com.gameconnect.game.dto;

import java.util.UUID;

import com.gameconnect.auth.entity.User.Position;
import com.gameconnect.auth.entity.User.SkillLevel;
import com.gameconnect.profile.dto.PlayerStatsResponse;

public record ApplicantSummary(
        UUID userId,
        String displayName,
        String profileImageUrl,
        SkillLevel skillLevel,
        Position position,
        PlayerStatsResponse stats
) {
}