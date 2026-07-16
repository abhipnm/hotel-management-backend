package com.restaurantmanager.dto.response;

import java.time.Instant;

public record AuthResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        UserSummaryResponse user
) {
    public static AuthResponse of(String token, Instant expiresAt, UserSummaryResponse user) {
        return new AuthResponse(token, "Bearer", expiresAt, user);
    }
}
