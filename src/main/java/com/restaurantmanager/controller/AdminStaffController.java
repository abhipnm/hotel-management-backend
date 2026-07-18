package com.restaurantmanager.controller;

import com.restaurantmanager.dto.request.CreateStaffRequest;
import com.restaurantmanager.dto.request.UpdateStaffRequest;
import com.restaurantmanager.dto.response.UserSummaryResponse;
import com.restaurantmanager.entity.AppUser;
import com.restaurantmanager.entity.Restaurant;
import com.restaurantmanager.repository.AppUserRepository;
import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.service.AuthService;
import com.restaurantmanager.service.RestaurantService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

@Tag(name = "Admin - Staff", description = "Manage kitchen/waiter staff accounts")
@RestController
@RequestMapping("/api/v1/admin/staff")
@RequiredArgsConstructor
public class AdminStaffController {

    private final AuthService authService;
    private final RestaurantService restaurantService;
    private final AppUserRepository appUserRepository;

    @GetMapping
    public ResponseEntity<List<UserSummaryResponse>> listStaff(@AuthenticationPrincipal AuthPrincipal principal) {
        List<UserSummaryResponse> staff = appUserRepository.findByRestaurantIdOrderByNameAsc(principal.restaurantId()).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
        return ResponseEntity.ok(staff);
    }

    @PostMapping
    public ResponseEntity<UserSummaryResponse> createStaff(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateStaffRequest request) {
        Restaurant restaurant = restaurantService.getById(principal.restaurantId());
        AppUser created = authService.createStaff(restaurant, request, principal.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(toSummary(created));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserSummaryResponse> updateStaff(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateStaffRequest request) {
        AppUser updated = authService.updateStaff(userId, principal.restaurantId(), principal.id(), request);
        return ResponseEntity.ok(toSummary(updated));
    }

    private UserSummaryResponse toSummary(AppUser user) {
        return new UserSummaryResponse(
                user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.getRestaurant().getId(), user.getRestaurant().getName(), user.isActive()
        );
    }
}
