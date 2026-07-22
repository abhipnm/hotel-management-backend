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

    // Anyone can call the unauthenticated public menu endpoint, so a competitor could poll it to
    // scrape exact inventory levels. The guest UI only ever needs the count to cap an order or show
    // a "running low" nudge - both only matter once stock is actually low - so healthy stock counts
    // (and the threshold that defines "low" for this item) are withheld from that view.
    private static final int DEFAULT_LOW_STOCK_VISIBILITY = 20;

    public static MenuItemResponse publicFrom(MenuItem item) {
        Integer stock = item.getStockQuantity();
        Integer threshold = item.getLowStockThreshold();
        boolean lowStock = stock != null && stock <= (threshold != null ? threshold : DEFAULT_LOW_STOCK_VISIBILITY);
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
                lowStock ? stock : null,
                null
        );
    }
}
