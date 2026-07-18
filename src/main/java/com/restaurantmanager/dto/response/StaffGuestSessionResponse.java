package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.GuestSession;

import java.time.Instant;
import java.util.UUID;

/** An active guest session as seen by staff/admin (e.g. tables awaiting payment). */
public record StaffGuestSessionResponse(
        UUID sessionId,
        String tableNumber,
        String guestName,
        boolean billRequested,
        Instant createdAt,
        /** Null when the guest didn't give a phone number. */
        Integer visitCount
) {
    public static StaffGuestSessionResponse from(GuestSession session, Integer visitCount) {
        return new StaffGuestSessionResponse(
                session.getId(),
                session.getTable().getTableNumber(),
                session.getGuestName(),
                session.isBillRequested(),
                session.getCreatedAt(),
                visitCount
        );
    }
}
