package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.GuestSession;

import java.time.Instant;
import java.util.UUID;

/** Broadcast over the /topic/restaurants/{restaurantId}/alerts websocket topic. */
public record StaffAlertEvent(
        Type type,
        UUID sessionId,
        String tableNumber,
        String guestName,
        Instant occurredAt
) {
    public enum Type {
        CALL_WAITER,
        BILL_REQUESTED
    }

    public static StaffAlertEvent callWaiter(GuestSession session) {
        return of(Type.CALL_WAITER, session);
    }

    public static StaffAlertEvent billRequested(GuestSession session) {
        return of(Type.BILL_REQUESTED, session);
    }

    private static StaffAlertEvent of(Type type, GuestSession session) {
        return new StaffAlertEvent(
                type,
                session.getId(),
                session.getTable().getTableNumber(),
                session.getGuestName(),
                Instant.now()
        );
    }
}
