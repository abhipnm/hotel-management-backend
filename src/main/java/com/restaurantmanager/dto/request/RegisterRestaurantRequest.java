package com.restaurantmanager.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRestaurantRequest(

        @NotBlank(message = "Restaurant name is required")
        @Size(max = 150)
        String restaurantName,

        @NotBlank(message = "Slug is required")
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug must be lowercase, alphanumeric and hyphen-separated")
        @Size(max = 80)
        String slug,

        @Size(max = 250)
        String address,

        @Size(max = 30)
        String phone,

        boolean vegOnly,

        @NotBlank(message = "Admin name is required")
        @Size(max = 100)
        String adminName,

        @NotBlank(message = "Admin email is required")
        @Email
        String adminEmail,

        @NotBlank(message = "Admin password is required")
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
        String adminPassword
) {
}
