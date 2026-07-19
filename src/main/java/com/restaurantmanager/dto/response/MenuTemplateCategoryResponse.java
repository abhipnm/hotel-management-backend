package com.restaurantmanager.dto.response;

import java.util.List;

public record MenuTemplateCategoryResponse(
        String key,
        String name,
        List<MenuTemplateItemResponse> items
) {
}
