package com.restaurantmanager.controller;

import com.restaurantmanager.dto.response.OrderResponse;
import com.restaurantmanager.entity.Order;
import com.restaurantmanager.entity.OrderStatus;
import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Full order history/reporting view for restaurant admins (all statuses, not just the live queue). */
@Tag(name = "Admin - Orders", description = "Full order history for the restaurant")
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false) List<OrderStatus> status) {
        List<OrderResponse> orders = orderService.listForRestaurant(principal.restaurantId(), status).stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID orderId) {
        Order order = orderService.getForRestaurant(orderId, principal.restaurantId());
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
