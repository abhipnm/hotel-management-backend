package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.Restaurant;

import java.util.UUID;

public record RestaurantResponse(
        UUID id,
        String name,
        String slug,
        String address,
        String phone,
        boolean active,
        boolean vegOnly
) {
    public static RestaurantResponse from(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getSlug(),
                restaurant.getAddress(),
                restaurant.getPhone(),
                restaurant.isActive(),
                restaurant.isVegOnly()
        );
    }
}
