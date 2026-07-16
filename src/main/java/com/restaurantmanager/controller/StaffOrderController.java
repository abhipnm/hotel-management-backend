package com.restaurantmanager.controller;

import com.restaurantmanager.dto.request.UpdateOrderStatusRequest;
import com.restaurantmanager.dto.response.OrderResponse;
import com.restaurantmanager.entity.Order;
import com.restaurantmanager.entity.OrderStatus;
import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Kitchen / waiter dashboard: view incoming orders and progress their
 * status. Reachable by both STAFF and ADMIN (see SecurityConfig).
 */
@Tag(name = "Staff", description = "Kitchen/waiter order queue")
@RestController
@RequestMapping("/api/v1/staff/orders")
@RequiredArgsConstructor
public class StaffOrderController {

    private static final Set<OrderStatus> DEFAULT_ACTIVE_STATUSES =
            EnumSet.of(OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY);

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false) List<OrderStatus> status) {

        Set<OrderStatus> statuses = (status == null || status.isEmpty())
                ? DEFAULT_ACTIVE_STATUSES
                : EnumSet.copyOf(status);

        List<OrderResponse> orders = orderService.listForRestaurant(principal.restaurantId(), statuses).stream()
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

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        Order order = orderService.updateStatus(orderId, principal.restaurantId(), request.status(), principal.id());
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
