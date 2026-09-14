package com.gameconnect.auth.dto;

import java.util.UUID;

import com.gameconnect.auth.entity.User.Position;
import com.gameconnect.auth.entity.User.SkillLevel;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String bio,
        String profileImageUrl,
        SkillLevel skillLevel,
        Position position
) {
}
