package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.Restaurant;
import com.restaurantmanager.entity.ThemeColor;

import java.util.UUID;

public record RestaurantResponse(
        UUID id,
        String name,
        String slug,
        String address,
        String phone,
        boolean active,
        boolean vegOnly,
        String logoUrl,
        ThemeColor themeColor,
        String tagline
) {
    public static RestaurantResponse from(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getSlug(),
                restaurant.getAddress(),
                restaurant.getPhone(),
                restaurant.isActive(),
                restaurant.isVegOnly(),
                restaurant.getLogoUrl(),
                restaurant.getThemeColor(),
                restaurant.getTagline()
        );
    }
}
