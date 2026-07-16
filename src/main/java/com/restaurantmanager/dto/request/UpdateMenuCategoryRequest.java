package com.restaurantmanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateMenuCategoryRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        int displayOrder,

        @NotNull
        Boolean active
) {
}
