package com.restaurantmanager.service;

import com.restaurantmanager.dto.request.AnalyticsPeriod;
import com.restaurantmanager.dto.response.SalesSummaryResponse;
import com.restaurantmanager.dto.response.SalesSummaryResponse.TopMenuItem;
import com.restaurantmanager.dto.response.SalesSummaryResponse.WaiterPerformance;
import com.restaurantmanager.repository.OrderItemRepository;
import com.restaurantmanager.repository.OrderItemRepository.TopItemProjection;
import com.restaurantmanager.repository.OrderRepository;
import com.restaurantmanager.repository.OrderRepository.WaiterPerformanceProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int ITEMS_LIMIT = 5;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public SalesSummaryResponse getSummary(UUID restaurantId, AnalyticsPeriod period) {
        Instant to = Instant.now();
        Instant from = period.from(to);
        Pageable topN = PageRequest.of(0, ITEMS_LIMIT);

        BigDecimal revenue = orderRepository.sumRevenue(restaurantId, from, to);
        long orderCount = orderRepository.countOrders(restaurantId, from, to);
        BigDecimal averageOrderValue = orderCount > 0
                ? revenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<TopMenuItem> topItems = toMenuItems(orderItemRepository.findTopSellingItems(restaurantId, from, to, topN));
        List<TopMenuItem> leastItems = toMenuItems(orderItemRepository.findLeastSellingItems(restaurantId, from, to, topN));

        Double avgServingSeconds = orderRepository.averageServingSeconds(restaurantId, from, to);
        Double averageServingTimeMinutes = avgServingSeconds != null
                ? BigDecimal.valueOf(avgServingSeconds / 60).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : null;

        List<WaiterPerformance> waiterPerformance = orderRepository.findWaiterPerformance(restaurantId, from, to).stream()
                .map(this::toWaiterPerformance)
                .collect(Collectors.toList());

        return new SalesSummaryResponse(
                from, to, revenue, orderCount, averageOrderValue, averageServingTimeMinutes,
                topItems, leastItems, waiterPerformance);
    }

    private List<TopMenuItem> toMenuItems(List<TopItemProjection> rows) {
        return rows.stream()
                .map(row -> new TopMenuItem(row.getName(), row.getQuantitySold(), row.getRevenue()))
                .collect(Collectors.toList());
    }

    private WaiterPerformance toWaiterPerformance(WaiterPerformanceProjection row) {
        double averageServingTimeMinutes = BigDecimal.valueOf(row.getAvgServingSeconds() / 60)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
        return new WaiterPerformance(row.getWaiterName(), row.getOrdersServed(), row.getRevenue(), averageServingTimeMinutes);
    }
}
