package com.restaurantmanager.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ApplyScannedMenuRequest(

        @NotEmpty
        @Valid
        List<ScannedMenuCategoryInput> categories
) {
}
