package com.restaurantmanager.controller;

import com.restaurantmanager.dto.request.CreateCouponRequest;
import com.restaurantmanager.dto.request.UpdateCouponRequest;
import com.restaurantmanager.dto.response.CouponResponse;
import com.restaurantmanager.entity.Coupon;
import com.restaurantmanager.entity.Restaurant;
import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.service.CouponService;
import com.restaurantmanager.service.RestaurantService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Admin - Coupons", description = "Manage discount coupons")
@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;
    private final RestaurantService restaurantService;

    @GetMapping
    public ResponseEntity<List<CouponResponse>> listCoupons(@AuthenticationPrincipal AuthPrincipal principal) {
        List<CouponResponse> coupons = couponService.listForRestaurant(principal.restaurantId()).stream()
                .map(CouponResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(coupons);
    }

    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateCouponRequest request) {
        Restaurant restaurant = restaurantService.getById(principal.restaurantId());
        Coupon coupon = couponService.create(restaurant, request, principal.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(CouponResponse.from(coupon));
    }

    @PutMapping("/{couponId}")
    public ResponseEntity<CouponResponse> updateCoupon(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID couponId,
            @Valid @RequestBody UpdateCouponRequest request) {
        Coupon coupon = couponService.update(couponId, principal.restaurantId(), request, principal.id());
        return ResponseEntity.ok(CouponResponse.from(coupon));
    }

    @DeleteMapping("/{couponId}")
    public ResponseEntity<Void> deleteCoupon(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID couponId) {
        couponService.delete(couponId, principal.restaurantId());
        return ResponseEntity.noContent().build();
    }
}
