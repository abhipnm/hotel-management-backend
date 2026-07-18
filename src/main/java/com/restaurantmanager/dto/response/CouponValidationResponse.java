package com.restaurantmanager.dto.response;

import java.math.BigDecimal;

public record CouponValidationResponse(
        String code,
        BigDecimal discountAmount,
        BigDecimal finalTotal
) {
}
