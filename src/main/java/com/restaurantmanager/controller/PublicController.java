package com.restaurantmanager.controller;

import com.restaurantmanager.dto.request.CreateGuestSessionRequest;
import com.restaurantmanager.dto.response.GuestSessionResponse;
import com.restaurantmanager.dto.response.MenuCategoryResponse;
import com.restaurantmanager.dto.response.PublicTableInfoResponse;
import com.restaurantmanager.entity.RestaurantTable;
import com.restaurantmanager.service.GuestSessionService;
import com.restaurantmanager.service.MenuService;
import com.restaurantmanager.service.TableService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Unauthenticated endpoints a guest's phone calls right after scanning a QR
 * code: resolve the table, browse the menu, and start a guest session.
 */
@Tag(name = "Public", description = "No-auth endpoints used by a guest's phone before/while ordering")
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicController {

    private final TableService tableService;
    private final MenuService menuService;
    private final GuestSessionService guestSessionService;

    @GetMapping("/qr/{qrToken}")
    public ResponseEntity<PublicTableInfoResponse> resolveQrCode(@PathVariable String qrToken) {
        RestaurantTable table = tableService.getByQrToken(qrToken);
        return ResponseEntity.ok(new PublicTableInfoResponse(
                table.getRestaurant().getId(),
                table.getRestaurant().getName(),
                table.getRestaurant().getSlug(),
                table.getRestaurant().isVegOnly(),
                table.getId(),
                table.getTableNumber()
        ));
    }

    @PostMapping("/guest-sessions")
    public ResponseEntity<GuestSessionResponse> createGuestSession(@Valid @RequestBody CreateGuestSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guestSessionService.createSession(request));
    }

    @GetMapping("/restaurants/{restaurantId}/menu")
    public ResponseEntity<List<MenuCategoryResponse>> getMenu(@PathVariable UUID restaurantId) {
        return ResponseEntity.ok(menuService.getPublicMenu(restaurantId));
    }
}
