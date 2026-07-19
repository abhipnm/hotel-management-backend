package com.restaurantmanager.controller;

import com.restaurantmanager.dto.request.ApplyMenuTemplateRequest;
import com.restaurantmanager.dto.response.MenuCategoryResponse;
import com.restaurantmanager.dto.response.MenuItemResponse;
import com.restaurantmanager.dto.response.MenuTemplateCategoryResponse;
import com.restaurantmanager.entity.Restaurant;
import com.restaurantmanager.repository.MenuItemRepository;
import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.service.MenuService;
import com.restaurantmanager.service.RestaurantService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.stream.Collectors;

/** Menu lookups and the quick-start template, available to both STAFF and ADMIN. */
@Tag(name = "Staff", description = "Available menu items and quick-start template")
@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffMenuController {

    private final MenuItemRepository menuItemRepository;
    private final MenuService menuService;
    private final RestaurantService restaurantService;

    @GetMapping("/menu")
    public ResponseEntity<List<MenuCategoryResponse>> getFullMenu(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(menuService.getFullMenu(principal.restaurantId()));
    }

    @GetMapping("/menu-items")
    public ResponseEntity<List<MenuItemResponse>> listAvailable(@AuthenticationPrincipal AuthPrincipal principal) {
        List<MenuItemResponse> items = menuItemRepository
                .findByRestaurantIdAndAvailableTrueOrderByDisplayOrderAsc(principal.restaurantId()).stream()
                .map(MenuItemResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    @GetMapping("/menu-template")
    public ResponseEntity<List<MenuTemplateCategoryResponse>> getMenuTemplate(@AuthenticationPrincipal AuthPrincipal principal) {
        Restaurant restaurant = restaurantService.getById(principal.restaurantId());
        return ResponseEntity.ok(menuService.getMenuTemplate(restaurant));
    }

    @PostMapping("/menu-template/apply")
    public ResponseEntity<List<MenuCategoryResponse>> applyMenuTemplate(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody ApplyMenuTemplateRequest request) {
        Restaurant restaurant = restaurantService.getById(principal.restaurantId());
        List<MenuCategoryResponse> menu = menuService.applyTemplate(restaurant, request.itemKeys(), principal.id());
        return ResponseEntity.ok(menu);
    }
}
