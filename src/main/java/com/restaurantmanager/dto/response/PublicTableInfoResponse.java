package com.restaurantmanager.dto.response;

import java.util.UUID;

/** What a guest's phone gets back right after scanning a QR code. */
public record PublicTableInfoResponse(
        UUID restaurantId,
        String restaurantName,
        String restaurantSlug,
        boolean restaurantVegOnly,
        UUID tableId,
        String tableNumber
) {
}
