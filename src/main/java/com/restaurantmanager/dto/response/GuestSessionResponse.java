package com.restaurantmanager.dto.response;

import java.time.Instant;
import java.util.UUID;

public record GuestSessionResponse(
        UUID sessionId,
        String guestToken,
        String tokenType,
        Instant expiresAt,
        String guestName,
        String restaurantName,
        String tableNumber
) {
}
