package com.restaurantmanager.dto.request;

import com.restaurantmanager.entity.ThemeColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateRestaurantRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 250)
        String address,

        @Size(max = 30)
        String phone,

        boolean vegOnly,

        @Size(max = 500)
        String logoUrl,

        @NotNull
        ThemeColor themeColor,

        @Size(max = 150)
        String tagline
) {
}
