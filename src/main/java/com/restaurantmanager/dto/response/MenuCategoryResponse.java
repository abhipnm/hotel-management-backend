package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.MenuCategory;

import java.util.List;
import java.util.UUID;

public record MenuCategoryResponse(
        UUID id,
        String name,
        int displayOrder,
        boolean active,
        List<MenuItemResponse> items
) {
    public static MenuCategoryResponse from(MenuCategory category, List<MenuItemResponse> items) {
        return new MenuCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDisplayOrder(),
                category.isActive(),
                items
        );
    }
}
