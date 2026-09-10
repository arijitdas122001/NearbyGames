package com.gameconnect.auth.dto;

import java.util.UUID;

<<<<<<< HEAD
import com.gameconnect.auth.entity.User.Position;
=======
>>>>>>> f43f156 (phase 2 done, frontend set up and authentication)
import com.gameconnect.auth.entity.User.SkillLevel;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String bio,
        String profileImageUrl,
<<<<<<< HEAD
        SkillLevel skillLevel,
        Position position
=======
        SkillLevel skillLevel
>>>>>>> f43f156 (phase 2 done, frontend set up and authentication)
) {
}
