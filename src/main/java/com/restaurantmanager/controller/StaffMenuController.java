package com.restaurantmanager.controller;

import com.restaurantmanager.dto.response.MenuItemResponse;
import com.restaurantmanager.repository.MenuItemRepository;
import com.restaurantmanager.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/** Read-only menu lookup for staff/admin — e.g. the "add item" picker when editing an order. */
@Tag(name = "Staff", description = "Available menu items")
@RestController
@RequestMapping("/api/v1/staff/menu-items")
@RequiredArgsConstructor
public class StaffMenuController {

    private final MenuItemRepository menuItemRepository;

    @GetMapping
    public ResponseEntity<List<MenuItemResponse>> listAvailable(@AuthenticationPrincipal AuthPrincipal principal) {
        List<MenuItemResponse> items = menuItemRepository
                .findByRestaurantIdAndAvailableTrueOrderByDisplayOrderAsc(principal.restaurantId()).stream()
                .map(MenuItemResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }
}
