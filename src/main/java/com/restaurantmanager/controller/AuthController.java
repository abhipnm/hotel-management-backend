package com.restaurantmanager.controller;

import com.restaurantmanager.dto.request.LoginRequest;
import com.restaurantmanager.dto.request.RegisterRestaurantRequest;
import com.restaurantmanager.dto.response.AuthResponse;
import com.restaurantmanager.security.LoginRateLimiterService;
import com.restaurantmanager.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Restaurant onboarding and staff login")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiterService loginRateLimiterService;

    @PostMapping("/register-restaurant")
    public ResponseEntity<AuthResponse> registerRestaurant(@Valid @RequestBody RegisterRestaurantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerRestaurant(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        loginRateLimiterService.checkAllowed(clientIp);
        try {
            AuthResponse response = authService.login(request);
            loginRateLimiterService.recordSuccess(clientIp);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            loginRateLimiterService.recordFailure(clientIp);
            throw e;
        }
    }
}
