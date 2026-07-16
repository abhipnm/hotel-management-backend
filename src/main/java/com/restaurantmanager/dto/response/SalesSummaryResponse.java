package com.restaurantmanager.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Sales summary for a reporting window on the restaurant admin dashboard. */
public record SalesSummaryResponse(
        Instant from,
        Instant to,
        BigDecimal totalRevenue,
        long orderCount,
        BigDecimal averageOrderValue,
        /** Null when no orders were served within the window yet. */
        Double averageServingTimeMinutes,
        List<TopMenuItem> topItems,
        List<TopMenuItem> leastItems,
        List<WaiterPerformance> waiterPerformance
) {
    public record TopMenuItem(String name, long quantitySold, BigDecimal revenue) {
    }

    public record WaiterPerformance(
            String waiterName, long ordersServed, BigDecimal revenue, double averageServingTimeMinutes) {
    }
}
