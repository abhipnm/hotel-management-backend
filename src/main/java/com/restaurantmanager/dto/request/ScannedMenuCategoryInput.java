package com.restaurantmanager.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ScannedMenuCategoryInput(

        @NotBlank
        @Size(max = 100)
        String name,

        @NotEmpty
        @Valid
        List<ScannedMenuItemInput> items
) {
}
