package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.GuestSession;
import com.restaurantmanager.entity.Order;
import com.restaurantmanager.entity.OrderStatus;
import com.restaurantmanager.entity.Restaurant;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Itemized bill/invoice for a guest session, as seen by staff before settling the table. */
public record BillResponse(
        UUID sessionId,
        String invoiceNumber,
        Instant issuedAt,
        String restaurantName,
        String restaurantAddress,
        String restaurantPhone,
        String tableNumber,
        String guestName,
        List<OrderResponse> orders,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal totalAmount
) {
    public static BillResponse from(GuestSession session, List<Order> orders) {
        List<Order> billable = orders.stream().filter(o -> o.getStatus() != OrderStatus.CANCELLED).toList();

        BigDecimal discount = billable.stream().map(Order::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = billable.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal subtotal = total.add(discount);

        Restaurant restaurant = session.getRestaurant();
        return new BillResponse(
                session.getId(),
                "INV-" + session.getId().toString().substring(0, 8).toUpperCase(),
                Instant.now(),
                restaurant.getName(),
                restaurant.getAddress(),
                restaurant.getPhone(),
                session.getTable().getTableNumber(),
                session.getGuestName(),
                orders.stream().map(OrderResponse::from).collect(Collectors.toList()),
                subtotal,
                discount,
                total
        );
    }
}
