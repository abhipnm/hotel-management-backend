package com.restaurantmanager.dto.request;

import com.restaurantmanager.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateCouponRequest(

        @NotBlank
        @Size(max = 30)
        String code,

        @NotNull
        DiscountType discountType,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal discountValue,

        /** Null means no minimum order amount is required. */
        @DecimalMin(value = "0.00")
        BigDecimal minOrderAmount,

        /** Null means unlimited uses. */
        @Min(1)
        Integer maxUses,

        /** Null means the coupon never expires. */
        Instant expiresAt
) {
}
