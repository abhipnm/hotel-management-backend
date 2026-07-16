package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID menuItemId,
        String itemName,
        BigDecimal price,
        int quantity,
        BigDecimal subtotal,
        String notes
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getMenuItem().getId(),
                item.getItemNameSnapshot(),
                item.getItemPriceSnapshot(),
                item.getQuantity(),
                item.getSubtotal(),
                item.getNotes()
        );
    }
}
