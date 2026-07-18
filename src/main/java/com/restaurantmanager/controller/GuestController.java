package com.restaurantmanager.controller;

import com.restaurantmanager.dto.request.CreateFeedbackRequest;
import com.restaurantmanager.dto.request.PlaceOrderRequest;
import com.restaurantmanager.dto.response.CouponValidationResponse;
import com.restaurantmanager.dto.response.FeedbackResponse;
import com.restaurantmanager.dto.response.GuestSessionSummaryResponse;
import com.restaurantmanager.dto.response.OrderResponse;
import com.restaurantmanager.entity.Coupon;
import com.restaurantmanager.entity.Feedback;
import com.restaurantmanager.entity.GuestSession;
import com.restaurantmanager.entity.Order;
import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.service.CouponService;
import com.restaurantmanager.service.FeedbackService;
import com.restaurantmanager.service.GuestSessionService;
import com.restaurantmanager.service.OrderService;
import com.restaurantmanager.service.StaffAlertService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Endpoints for an in-progress guest session (identified by the guest JWT
 * issued from POST /api/v1/public/guest-sessions).
 */
@Tag(name = "Guest", description = "Ordering endpoints for an authenticated guest session")
@RestController
@RequestMapping("/api/v1/guest")
@RequiredArgsConstructor
public class GuestController {

    private final OrderService orderService;
    private final GuestSessionService guestSessionService;
    private final StaffAlertService staffAlertService;
    private final FeedbackService feedbackService;
    private final CouponService couponService;

    @GetMapping("/session")
    public ResponseEntity<GuestSessionSummaryResponse> getSession(@AuthenticationPrincipal AuthPrincipal principal) {
        GuestSession session = guestSessionService.requireActiveSession(principal.id(), principal.restaurantId());
        return ResponseEntity.ok(GuestSessionSummaryResponse.from(session));
    }

    @PostMapping("/call-waiter")
    public ResponseEntity<Void> callWaiter(@AuthenticationPrincipal AuthPrincipal principal) {
        GuestSession session = guestSessionService.requireActiveSession(principal.id(), principal.restaurantId());
        staffAlertService.broadcastCallWaiter(session);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/session/request-bill")
    public ResponseEntity<GuestSessionSummaryResponse> requestBill(@AuthenticationPrincipal AuthPrincipal principal) {
        GuestSession session = guestSessionService.requestBill(principal.id(), principal.restaurantId());
        return ResponseEntity.ok(GuestSessionSummaryResponse.from(session));
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> placeOrder(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody PlaceOrderRequest request) {
        GuestSession session = guestSessionService.requireActiveSession(principal.id(), principal.restaurantId());
        Order order = orderService.placeOrder(session, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> myOrders(@AuthenticationPrincipal AuthPrincipal principal) {
        List<OrderResponse> orders = orderService.listForGuestSession(principal.id()).stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID orderId) {
        Order order = orderService.getForGuestSession(orderId, principal.id());
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    /** Preview-only: computes the discount without recording a use, so the cart can show a live total before the guest places the order. */
    @GetMapping("/coupons/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam String code,
            @RequestParam BigDecimal subtotal) {
        Coupon coupon = couponService.findValid(principal.restaurantId(), code, subtotal);
        BigDecimal discountAmount = couponService.calculateDiscount(coupon, subtotal);
        return ResponseEntity.ok(new CouponValidationResponse(
                coupon.getCode(), discountAmount, subtotal.subtract(discountAmount)));
    }

    @PostMapping("/feedback")
    public ResponseEntity<FeedbackResponse> submitFeedback(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateFeedbackRequest request) {
        GuestSession session = guestSessionService.requireActiveSession(principal.id(), principal.restaurantId());
        Feedback feedback = feedbackService.submit(session, request);
        return ResponseEntity.ok(FeedbackResponse.from(feedback));
    }

    @GetMapping("/feedback")
    public ResponseEntity<FeedbackResponse> myFeedback(@AuthenticationPrincipal AuthPrincipal principal) {
        return feedbackService.findForSession(principal.id())
                .map(FeedbackResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
