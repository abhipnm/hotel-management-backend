package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.GuestSession;
import com.restaurantmanager.entity.RestaurantTable;

import java.util.UUID;

/** A table as seen on the staff floor view: current occupancy plus who's sitting there, if anyone. */
public record StaffTableResponse(
        UUID id,
        String tableNumber,
        boolean active,
        boolean occupied,
        UUID sessionId,
        String guestName,
        boolean billRequested
) {
    public static StaffTableResponse from(RestaurantTable table, GuestSession activeSession) {
        return new StaffTableResponse(
                table.getId(),
                table.getTableNumber(),
                table.isActive(),
                activeSession != null,
                activeSession != null ? activeSession.getId() : null,
                activeSession != null ? activeSession.getGuestName() : null,
                activeSession != null && activeSession.isBillRequested()
        );
    }
}
