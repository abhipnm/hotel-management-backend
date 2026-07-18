package com.restaurantmanager.controller;

import com.restaurantmanager.dto.response.ActivityLogResponse;
import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.service.ActivityLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Admin - Activity log", description = "Recent staff-initiated changes, for accountability")
@RestController
@RequestMapping("/api/v1/admin/activity-log")
@RequiredArgsConstructor
public class AdminActivityLogController {

    private static final int MAX_ENTRIES = 100;

    private final ActivityLogService activityLogService;

    @GetMapping
    public ResponseEntity<List<ActivityLogResponse>> listRecent(@AuthenticationPrincipal AuthPrincipal principal) {
        List<ActivityLogResponse> entries = activityLogService.listRecent(principal.restaurantId(), MAX_ENTRIES).stream()
                .map(ActivityLogResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(entries);
    }
}
