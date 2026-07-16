package com.restaurantmanager.controller;

import com.restaurantmanager.dto.response.NotificationResponse;
import com.restaurantmanager.dto.response.UnreadCountResponse;
import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Staff", description = "Notification center")
@RestController
@RequestMapping("/api/v1/staff/notifications")
@RequiredArgsConstructor
public class StaffNotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(notificationService.listForStaff(principal.restaurantId()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(new UnreadCountResponse(notificationService.unreadCountForStaff(principal.restaurantId())));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal AuthPrincipal principal) {
        notificationService.markAllStaffRead(principal.restaurantId());
        return ResponseEntity.noContent().build();
    }
}
