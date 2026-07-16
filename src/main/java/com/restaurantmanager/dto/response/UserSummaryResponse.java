package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.Role;

import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String name,
        String email,
        Role role,
        UUID restaurantId,
        String restaurantName,
        boolean active
) {
}
