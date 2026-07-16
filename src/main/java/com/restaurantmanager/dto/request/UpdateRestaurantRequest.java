package com.restaurantmanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRestaurantRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 250)
        String address,

        @Size(max = 30)
        String phone,

        boolean vegOnly
) {
}
