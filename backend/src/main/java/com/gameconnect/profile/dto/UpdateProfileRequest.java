package com.gameconnect.profile.dto;

import com.gameconnect.auth.entity.User.Position;
import com.gameconnect.auth.entity.User.SkillLevel;

import jakarta.validation.constraints.Size;

/**
 * Partial profile update request. All fields are optional; only non-null
 * fields are applied by the service.
 */
public record UpdateProfileRequest(
        @Size(max = 80, message = "Display name must be at most 80 characters")
        String displayName,

        @Size(max = 2000, message = "Bio must be at most 2000 characters")
        String bio,

        @Size(max = 500, message = "Profile image URL must be at most 500 characters")
        String profileImageUrl,

        SkillLevel skillLevel,

        Position position
) {

    public boolean isEmpty() {
        return displayName == null
                && bio == null
                && profileImageUrl == null
                && skillLevel == null
                && position == null;
    }
}