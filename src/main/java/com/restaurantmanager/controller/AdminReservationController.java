package com.restaurantmanager.controller;

import com.restaurantmanager.dto.request.AssignReservationTableRequest;
import com.restaurantmanager.dto.request.CreateReservationRequest;
import com.restaurantmanager.dto.request.UpdateReservationStatusRequest;
import com.restaurantmanager.dto.response.ReservationResponse;
import com.restaurantmanager.entity.Reservation;
import com.restaurantmanager.entity.ReservationStatus;
import com.restaurantmanager.entity.Restaurant;
import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.service.ReservationService;
import com.restaurantmanager.service.RestaurantService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Admin - Reservations", description = "Manage table reservations")
@RestController
@RequestMapping("/api/v1/admin/reservations")
@RequiredArgsConstructor
public class AdminReservationController {

    private final ReservationService reservationService;
    private final RestaurantService restaurantService;

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> listReservations(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false) List<ReservationStatus> status) {
        List<ReservationResponse> reservations = reservationService.listForRestaurant(principal.restaurantId(), status).stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(reservations);
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateReservationRequest request) {
        Restaurant restaurant = restaurantService.getById(principal.restaurantId());
        Reservation reservation = reservationService.create(restaurant, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservationResponse.from(reservation));
    }

    @PatchMapping("/{reservationId}/status")
    public ResponseEntity<ReservationResponse> updateStatus(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID reservationId,
            @Valid @RequestBody UpdateReservationStatusRequest request) {
        Reservation reservation = reservationService.updateStatus(reservationId, principal.restaurantId(), request.status());
        return ResponseEntity.ok(ReservationResponse.from(reservation));
    }

    @PatchMapping("/{reservationId}/table")
    public ResponseEntity<ReservationResponse> assignTable(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID reservationId,
            @Valid @RequestBody AssignReservationTableRequest request) {
        Reservation reservation = reservationService.assignTable(reservationId, principal.restaurantId(), request.tableId());
        return ResponseEntity.ok(ReservationResponse.from(reservation));
    }
}
