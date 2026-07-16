package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.Order;
import com.restaurantmanager.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record OrderResponse(
        UUID id,
        UUID tableId,
        String tableNumber,
        String guestName,
        OrderStatus status,
        BigDecimal totalAmount,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemResponse> items,
        /** Null until the order is marked SERVED. */
        String servedByName
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getTable().getId(),
                order.getTable().getTableNumber(),
                order.getGuestSession().getGuestName(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems().stream().map(OrderItemResponse::from).collect(Collectors.toList()),
                order.getServedBy() != null ? order.getServedBy().getName() : null
        );
    }
}
