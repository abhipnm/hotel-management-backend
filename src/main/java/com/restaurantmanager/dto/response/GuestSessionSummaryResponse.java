package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.GuestSession;

import java.time.Instant;
import java.util.UUID;

public record GuestSessionSummaryResponse(
        UUID sessionId,
        String guestName,
        String tableNumber,
        String restaurantName,
        String status,
        Instant expiresAt,
        boolean billRequested,
        boolean paid
) {
    public static GuestSessionSummaryResponse from(GuestSession session) {
        return new GuestSessionSummaryResponse(
                session.getId(),
                session.getGuestName(),
                session.getTable().getTableNumber(),
                session.getRestaurant().getName(),
                session.getStatus().name(),
                session.getExpiresAt(),
                session.isBillRequested(),
                session.getPaidAt() != null
        );
    }
}
