package com.restaurantmanager.dto.request;

import com.restaurantmanager.entity.FoodType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateMenuItemRequest(

        @NotNull
        UUID categoryId,

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 1000)
        String description,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
        BigDecimal price,

        String imageUrl,

        @NotNull
        FoodType foodType,

        int displayOrder,

        /** Null means stock isn't tracked for this item. */
        @Min(value = 0, message = "Stock quantity cannot be negative")
        Integer stockQuantity,

        @Min(value = 0, message = "Low stock threshold cannot be negative")
        Integer lowStockThreshold
) {
}
