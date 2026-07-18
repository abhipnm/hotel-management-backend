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
        boolean billRequested,
        /** Null when the seated guest didn't give a phone number, or the table is unoccupied. */
        Integer visitCount,
        /** Null when no waiter is assigned to this table. */
        String assignedWaiterName
) {
    public static StaffTableResponse from(RestaurantTable table, GuestSession activeSession, Integer visitCount) {
        return new StaffTableResponse(
                table.getId(),
                table.getTableNumber(),
                table.isActive(),
                activeSession != null,
                activeSession != null ? activeSession.getId() : null,
                activeSession != null ? activeSession.getGuestName() : null,
                activeSession != null && activeSession.isBillRequested(),
                visitCount,
                table.getAssignedWaiter() != null ? table.getAssignedWaiter().getName() : null
        );
    }
}
