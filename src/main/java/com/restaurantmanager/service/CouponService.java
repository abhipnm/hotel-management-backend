package com.restaurantmanager.service;

import com.restaurantmanager.dto.request.CreateCouponRequest;
import com.restaurantmanager.dto.request.UpdateCouponRequest;
import com.restaurantmanager.entity.Coupon;
import com.restaurantmanager.entity.DiscountType;
import com.restaurantmanager.entity.Restaurant;
import com.restaurantmanager.exception.BadRequestException;
import com.restaurantmanager.exception.ConflictException;
import com.restaurantmanager.exception.ResourceNotFoundException;
import com.restaurantmanager.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final ActivityLogService activityLogService;

    @Transactional
    public Coupon create(Restaurant restaurant, CreateCouponRequest request, UUID actorId) {
        if (couponRepository.existsByRestaurantIdAndCodeIgnoreCase(restaurant.getId(), request.code())) {
            throw new ConflictException("Coupon code '" + request.code() + "' already exists for this restaurant");
        }
        Coupon coupon = Coupon.builder()
                .restaurant(restaurant)
                .code(request.code().toUpperCase())
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .minOrderAmount(request.minOrderAmount())
                .maxUses(request.maxUses())
                .expiresAt(request.expiresAt())
                .active(true)
                .usedCount(0)
                .build();
        coupon = couponRepository.save(coupon);
        activityLogService.log(restaurant.getId(), actorId, "COUPON_CREATED", "Created coupon " + coupon.getCode());
        return coupon;
    }

    @Transactional(readOnly = true)
    public List<Coupon> listForRestaurant(UUID restaurantId) {
        return couponRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId);
    }

    @Transactional
    public Coupon update(UUID couponId, UUID restaurantId, UpdateCouponRequest request, UUID actorId) {
        Coupon coupon = getForRestaurant(couponId, restaurantId);
        boolean wasActive = coupon.isActive();
        coupon.setCode(request.code().toUpperCase());
        coupon.setDiscountType(request.discountType());
        coupon.setDiscountValue(request.discountValue());
        coupon.setMinOrderAmount(request.minOrderAmount());
        coupon.setMaxUses(request.maxUses());
        coupon.setActive(request.active());
        coupon.setExpiresAt(request.expiresAt());

        if (wasActive && !request.active()) {
            activityLogService.log(restaurantId, actorId, "COUPON_DEACTIVATED", "Deactivated coupon " + coupon.getCode());
        } else if (!wasActive && request.active()) {
            activityLogService.log(restaurantId, actorId, "COUPON_REACTIVATED", "Reactivated coupon " + coupon.getCode());
        } else {
            activityLogService.log(restaurantId, actorId, "COUPON_UPDATED", "Updated coupon " + coupon.getCode());
        }
        return coupon;
    }

    @Transactional
    public void delete(UUID couponId, UUID restaurantId) {
        Coupon coupon = getForRestaurant(couponId, restaurantId);
        couponRepository.delete(coupon);
    }

    /** Read-only check used both for the guest-facing preview and just before an order is placed. */
    @Transactional(readOnly = true)
    public Coupon findValid(UUID restaurantId, String code, BigDecimal orderSubtotal) {
        Coupon coupon = couponRepository.findByRestaurantIdAndCodeIgnoreCase(restaurantId, code)
                .orElseThrow(() -> new BadRequestException("Coupon '" + code + "' is not valid"));

        if (!coupon.isActive()) {
            throw new BadRequestException("Coupon '" + code + "' is no longer active");
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Coupon '" + code + "' has expired");
        }
        if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
            throw new BadRequestException("Coupon '" + code + "' has reached its usage limit");
        }
        if (coupon.getMinOrderAmount() != null && orderSubtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BadRequestException(
                    "Coupon '" + code + "' requires a minimum order of " + coupon.getMinOrderAmount());
        }
        return coupon;
    }

    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderSubtotal) {
        BigDecimal discount = coupon.getDiscountType() == DiscountType.PERCENTAGE
                ? orderSubtotal.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : coupon.getDiscountValue();
        return discount.min(orderSubtotal);
    }

    /** Called only when an order is actually placed with this coupon — never on preview. */
    @Transactional
    public void recordUsage(Coupon coupon) {
        coupon.setUsedCount(coupon.getUsedCount() + 1);
    }

    private Coupon getForRestaurant(UUID couponId, UUID restaurantId) {
        return couponRepository.findByIdAndRestaurantId(couponId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + couponId));
    }
}
