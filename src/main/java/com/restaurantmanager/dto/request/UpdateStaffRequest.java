package com.restaurantmanager.dto.request;

import com.restaurantmanager.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStaffRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        Role role,

        boolean active
) {
}
