package com.gameconnect.auth.dto;

import java.util.UUID;

public record AuthResponse(
        UUID id,
        String email,
        String displayName
) {
}
