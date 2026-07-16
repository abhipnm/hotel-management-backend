package com.restaurantmanager.controller;

import com.restaurantmanager.dto.request.CreateMenuCategoryRequest;
import com.restaurantmanager.dto.request.CreateMenuItemRequest;
import com.restaurantmanager.dto.request.UpdateMenuCategoryRequest;
import com.restaurantmanager.dto.request.UpdateMenuItemRequest;
import com.restaurantmanager.dto.response.MenuCategoryResponse;
import com.restaurantmanager.dto.response.MenuItemResponse;
import com.restaurantmanager.entity.MenuCategory;
import com.restaurantmanager.entity.MenuItem;
import com.restaurantmanager.entity.Restaurant;
import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.service.MenuService;
import com.restaurantmanager.service.RestaurantService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Admin - Menu", description = "Manage menu categories and items")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminMenuController {

    private final MenuService menuService;
    private final RestaurantService restaurantService;

    @GetMapping("/menu")
    public ResponseEntity<List<MenuCategoryResponse>> getFullMenu(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(menuService.getFullMenu(principal.restaurantId()));
    }

    // ---- Categories ----

    @GetMapping("/menu-categories")
    public ResponseEntity<List<MenuCategoryResponse>> listCategories(@AuthenticationPrincipal AuthPrincipal principal) {
        List<MenuCategoryResponse> categories = menuService.listCategories(principal.restaurantId()).stream()
                .map(c -> MenuCategoryResponse.from(c, List.of()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(categories);
    }

    @PostMapping("/menu-categories")
    public ResponseEntity<MenuCategoryResponse> createCategory(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateMenuCategoryRequest request) {
        Restaurant restaurant = restaurantService.getById(principal.restaurantId());
        MenuCategory category = menuService.createCategory(restaurant, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MenuCategoryResponse.from(category, List.of()));
    }

    @PutMapping("/menu-categories/{categoryId}")
    public ResponseEntity<MenuCategoryResponse> updateCategory(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateMenuCategoryRequest request) {
        MenuCategory category = menuService.updateCategory(categoryId, principal.restaurantId(), request);
        return ResponseEntity.ok(MenuCategoryResponse.from(category, List.of()));
    }

    @DeleteMapping("/menu-categories/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID categoryId) {
        menuService.deleteCategory(categoryId, principal.restaurantId());
        return ResponseEntity.noContent().build();
    }

    // ---- Items ----

    @GetMapping("/menu-items")
    public ResponseEntity<List<MenuItemResponse>> listItems(@AuthenticationPrincipal AuthPrincipal principal) {
        List<MenuItemResponse> items = menuService.listItems(principal.restaurantId()).stream()
                .map(MenuItemResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    @PostMapping("/menu-items")
    public ResponseEntity<MenuItemResponse> createItem(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateMenuItemRequest request) {
        Restaurant restaurant = restaurantService.getById(principal.restaurantId());
        MenuItem item = menuService.createItem(restaurant, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MenuItemResponse.from(item));
    }

    @PutMapping("/menu-items/{itemId}")
    public ResponseEntity<MenuItemResponse> updateItem(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateMenuItemRequest request) {
        MenuItem item = menuService.updateItem(itemId, principal.restaurantId(), request);
        return ResponseEntity.ok(MenuItemResponse.from(item));
    }

    @DeleteMapping("/menu-items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID itemId) {
        menuService.deleteItem(itemId, principal.restaurantId());
        return ResponseEntity.noContent().build();
    }
}
