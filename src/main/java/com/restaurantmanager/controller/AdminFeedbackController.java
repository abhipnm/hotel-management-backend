package com.restaurantmanager.controller;

import com.restaurantmanager.dto.response.FeedbackSummaryResponse;
import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.service.FeedbackService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Guest feedback overview for restaurant admins. */
@Tag(name = "Admin - Feedback", description = "Guest ratings and comments")
@RestController
@RequestMapping("/api/v1/admin/feedback")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping
    public ResponseEntity<FeedbackSummaryResponse> getFeedback(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(feedbackService.getSummary(principal.restaurantId()));
    }
}
