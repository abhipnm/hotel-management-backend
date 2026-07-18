package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.Coupon;
import com.restaurantmanager.entity.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponResponse(
        UUID id,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minOrderAmount,
        Integer maxUses,
        int usedCount,
        boolean active,
        Instant expiresAt,
        Instant createdAt
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinOrderAmount(),
                coupon.getMaxUses(),
                coupon.getUsedCount(),
                coupon.isActive(),
                coupon.getExpiresAt(),
                coupon.getCreatedAt()
        );
    }
}
