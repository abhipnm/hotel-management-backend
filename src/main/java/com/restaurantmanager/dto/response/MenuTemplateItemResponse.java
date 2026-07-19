package com.restaurantmanager.dto.response;

import com.restaurantmanager.catalog.MenuTemplateCatalog.TemplateItem;
import com.restaurantmanager.entity.FoodType;

import java.math.BigDecimal;

public record MenuTemplateItemResponse(
        String key,
        String name,
        String description,
        BigDecimal price,
        FoodType foodType
) {
    public static MenuTemplateItemResponse from(TemplateItem item) {
        return new MenuTemplateItemResponse(item.key(), item.name(), item.description(), item.price(), item.foodType());
    }
}
