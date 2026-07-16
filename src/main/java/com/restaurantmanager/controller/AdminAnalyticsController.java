package com.restaurantmanager.controller;

import com.restaurantmanager.dto.request.AnalyticsPeriod;
import com.restaurantmanager.dto.response.SalesSummaryResponse;
import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.service.AnalyticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Sales reporting for restaurant admins. */
@Tag(name = "Admin - Analytics", description = "Sales summary reporting")
@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public ResponseEntity<SalesSummaryResponse> getSummary(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "DAY") AnalyticsPeriod period) {
        return ResponseEntity.ok(analyticsService.getSummary(principal.restaurantId(), period));
    }
}
