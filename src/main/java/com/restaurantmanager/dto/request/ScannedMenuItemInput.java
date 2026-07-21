package com.restaurantmanager.dto.request;

import com.restaurantmanager.entity.FoodType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** One item as extracted from a scanned menu photo, or as edited by the admin before applying. */
public record ScannedMenuItemInput(

        @NotBlank
        String name,

        String description,

        @NotNull
        @DecimalMin(value = "0", inclusive = true)
        BigDecimal price,

        @NotNull
        FoodType foodType
) {
}
