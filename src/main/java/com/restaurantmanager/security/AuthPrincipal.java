package com.restaurantmanager.security;

import java.util.UUID;

/**
 * The authenticated principal attached to the SecurityContext for every
 * request. Covers both staff (ADMIN/STAFF) and guest (GUEST) callers so a
 * single JWT filter chain can serve both audiences.
 */
public record AuthPrincipal(
        UUID id,
        UUID restaurantId,
        UUID tableId,
        PrincipalType type,
        String role
) {
    public enum PrincipalType {
        STAFF,
        GUEST
    }

    public boolean isGuest() {
        return type == PrincipalType.GUEST;
    }
}
