package com.restaurantmanager.dto.request;

import com.restaurantmanager.entity.FoodType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * One item as extracted from a scanned menu photo, or as edited by the admin before applying.
 * These bounds mirror the menu_items table (name/description column lengths, NUMERIC(10,2) price) so
 * a hallucinated or tampered value fails validation with a clean 400 instead of a raw DB error.
 */
public record ScannedMenuItemInput(

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 1000)
        String description,

        @NotNull
        @DecimalMin(value = "0", inclusive = true)
        @DecimalMax(value = "99999999.99", inclusive = true)
        BigDecimal price,

        @NotNull
        FoodType foodType
) {
}
