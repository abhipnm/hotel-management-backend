package com.restaurantmanager.controller;

import com.restaurantmanager.dto.request.UpdateRestaurantRequest;
import com.restaurantmanager.dto.response.RestaurantResponse;
import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.service.RestaurantService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin - Restaurant", description = "Manage the restaurant's own profile")
@RestController
@RequestMapping("/api/v1/admin/restaurant")
@RequiredArgsConstructor
public class AdminRestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping
    public ResponseEntity<RestaurantResponse> getRestaurant(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(RestaurantResponse.from(restaurantService.getById(principal.restaurantId())));
    }

    @PatchMapping
    public ResponseEntity<RestaurantResponse> updateRestaurant(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody UpdateRestaurantRequest request) {
        return ResponseEntity.ok(RestaurantResponse.from(restaurantService.update(principal.restaurantId(), request)));
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RestaurantResponse> uploadLogo(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(RestaurantResponse.from(restaurantService.updateLogo(principal.restaurantId(), file)));
    }

    @DeleteMapping("/logo")
    public ResponseEntity<RestaurantResponse> deleteLogo(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(RestaurantResponse.from(restaurantService.clearLogo(principal.restaurantId())));
    }
}
