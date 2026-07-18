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
        String tableNumber,
        /** Null when no phone was given; otherwise how many visits this phone has made, including this one. */
        Integer visitCount
) {
}
