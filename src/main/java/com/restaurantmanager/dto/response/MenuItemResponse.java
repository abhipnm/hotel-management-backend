package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.FoodType;
import com.restaurantmanager.entity.MenuItem;

import java.math.BigDecimal;
import java.util.UUID;

public record MenuItemResponse(
        UUID id,
        UUID categoryId,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        FoodType foodType,
        boolean available,
        int displayOrder,
        Integer stockQuantity,
        Integer lowStockThreshold
) {
    public static MenuItemResponse from(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getCategory().getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getImageUrl(),
                item.getFoodType(),
                item.isAvailable(),
                item.getDisplayOrder(),
                item.getStockQuantity(),
                item.getLowStockThreshold()
        );
    }
}
