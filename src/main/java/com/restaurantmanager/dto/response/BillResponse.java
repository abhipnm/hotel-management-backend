package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.GuestSession;
import com.restaurantmanager.entity.Order;
import com.restaurantmanager.entity.OrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Itemized bill for a guest session, as seen by staff before settling the table. */
public record BillResponse(
        UUID sessionId,
        String tableNumber,
        String guestName,
        List<OrderResponse> orders,
        BigDecimal totalAmount
) {
    public static BillResponse from(GuestSession session, List<Order> orders) {
        BigDecimal total = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BillResponse(
                session.getId(),
                session.getTable().getTableNumber(),
                session.getGuestName(),
                orders.stream().map(OrderResponse::from).collect(Collectors.toList()),
                total
        );
    }
}
