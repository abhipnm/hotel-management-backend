package com.restaurantmanager.controller;

import com.restaurantmanager.dto.request.CreateAnnouncementRequest;
import com.restaurantmanager.entity.Restaurant;
import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.service.NotificationService;
import com.restaurantmanager.service.RestaurantService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-authored broadcast messages, fanned out to both the staff dashboard and every current guest. */
@Tag(name = "Admin - Announcements", description = "Broadcast a message to staff and guests")
@RestController
@RequestMapping("/api/v1/admin/announcements")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final RestaurantService restaurantService;
    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<Void> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateAnnouncementRequest request) {
        Restaurant restaurant = restaurantService.getById(principal.restaurantId());
        notificationService.createAnnouncement(restaurant, request.message());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
