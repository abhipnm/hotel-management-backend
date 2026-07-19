package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.RestaurantTable;

import java.util.List;
import java.util.UUID;

/** A table as seen on the staff floor view: current occupancy plus every guest currently seated there, if any. */
public record StaffTableResponse(
        UUID id,
        String tableNumber,
        boolean active,
        boolean occupied,
        /** Every active guest session at this table — a table can have more than one when guests order under separate names. */
        List<StaffTableGuestResponse> guests,
        /** Null when no waiter is assigned to this table. */
        String assignedWaiterName
) {
    public static StaffTableResponse from(RestaurantTable table, List<StaffTableGuestResponse> guests) {
        return new StaffTableResponse(
                table.getId(),
                table.getTableNumber(),
                table.isActive(),
                !guests.isEmpty(),
                guests,
                table.getAssignedWaiter() != null ? table.getAssignedWaiter().getName() : null
        );
    }
}
