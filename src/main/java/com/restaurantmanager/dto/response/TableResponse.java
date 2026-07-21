package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.RestaurantTable;

import java.util.UUID;

public record TableResponse(
        UUID id,
        String tableNumber,
        Integer capacity,
        String qrToken,
        String qrCodeImageUrl,
        boolean active,
        boolean occupied,
        UUID assignedWaiterId,
        String assignedWaiterName
) {
    /** A table on its own, outside a session-aware listing (e.g. right after create/update) — never occupied by definition. */
    public static TableResponse from(RestaurantTable table) {
        return from(table, false);
    }

    public static TableResponse from(RestaurantTable table, boolean occupied) {
        return new TableResponse(
                table.getId(),
                table.getTableNumber(),
                table.getCapacity(),
                table.getQrToken(),
                "/api/v1/admin/tables/" + table.getId() + "/qrcode",
                table.isActive(),
                occupied,
                table.getAssignedWaiter() != null ? table.getAssignedWaiter().getId() : null,
                table.getAssignedWaiter() != null ? table.getAssignedWaiter().getName() : null
        );
    }
}
