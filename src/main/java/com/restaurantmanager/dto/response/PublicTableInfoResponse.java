package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.ThemeColor;

import java.util.UUID;

/** What a guest's phone gets back right after scanning a QR code. */
public record PublicTableInfoResponse(
        UUID restaurantId,
        String restaurantName,
        String restaurantSlug,
        boolean restaurantVegOnly,
        /** Null when the restaurant hasn't set a logo. */
        String restaurantLogoUrl,
        ThemeColor restaurantThemeColor,
        /** Null when the restaurant hasn't set a tagline. */
        String restaurantTagline,
        UUID tableId,
        String tableNumber
) {
}
