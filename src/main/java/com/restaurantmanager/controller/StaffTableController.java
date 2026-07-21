package com.restaurantmanager.controller;

import com.restaurantmanager.dto.request.BookTableRequest;
import com.restaurantmanager.dto.request.BulkCreateTablesRequest;
import com.restaurantmanager.dto.request.CreateTableRequest;
import com.restaurantmanager.dto.request.SetTableActiveRequest;
import com.restaurantmanager.dto.response.ReservationResponse;
import com.restaurantmanager.dto.response.StaffTableGuestResponse;
import com.restaurantmanager.dto.response.StaffTableResponse;
import com.restaurantmanager.dto.response.TableResponse;
import com.restaurantmanager.entity.GuestSession;
import com.restaurantmanager.entity.Reservation;
import com.restaurantmanager.entity.Restaurant;
import com.restaurantmanager.entity.RestaurantTable;
import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.service.GuestSessionService;
import com.restaurantmanager.service.ReservationService;
import com.restaurantmanager.service.RestaurantService;
import com.restaurantmanager.service.TableService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Floor view for waitstaff: live occupancy, plus the add/block/book actions available to STAFF and ADMIN alike. */
@Tag(name = "Staff", description = "Floor view of tables, and add/block/book actions")
@RestController
@RequestMapping("/api/v1/staff/tables")
@RequiredArgsConstructor
public class StaffTableController {

    private final TableService tableService;
    private final GuestSessionService guestSessionService;
    private final ReservationService reservationService;
    private final RestaurantService restaurantService;

    @GetMapping
    public ResponseEntity<List<StaffTableResponse>> listTables(@AuthenticationPrincipal AuthPrincipal principal) {
        // Grouped, not deduped — a table can have more than one active session when guests order under separate names.
        Map<UUID, List<GuestSession>> sessionsByTableId = guestSessionService.listActiveSessions(principal.restaurantId()).stream()
                .collect(Collectors.groupingBy(s -> s.getTable().getId()));

        List<StaffTableResponse> tables = tableService.listForRestaurant(principal.restaurantId()).stream()
                .map(table -> {
                    List<GuestSession> sessions = sessionsByTableId.getOrDefault(table.getId(), List.of());
                    List<StaffTableGuestResponse> guests = sessions.stream()
                            .map(session -> new StaffTableGuestResponse(
                                    session.getId(),
                                    session.getGuestName(),
                                    session.isBillRequested(),
                                    guestSessionService.getVisitCount(session)))
                            .collect(Collectors.toList());
                    return StaffTableResponse.from(table, guests);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(tables);
    }

    @PostMapping
    public ResponseEntity<TableResponse> createTable(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateTableRequest request) {
        Restaurant restaurant = restaurantService.getById(principal.restaurantId());
        RestaurantTable table = tableService.create(restaurant, request, principal.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(TableResponse.from(table));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<TableResponse>> createTablesBulk(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody BulkCreateTablesRequest request) {
        Restaurant restaurant = restaurantService.getById(principal.restaurantId());
        List<TableResponse> tables = tableService.createBulk(restaurant, request.tables(), principal.id()).stream()
                .map(TableResponse::from)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(tables);
    }

    @PatchMapping("/{tableId}/active")
    public ResponseEntity<TableResponse> setActive(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID tableId,
            @Valid @RequestBody SetTableActiveRequest request) {
        RestaurantTable table = tableService.setActive(tableId, principal.restaurantId(), request.active(), principal.id());
        return ResponseEntity.ok(TableResponse.from(table));
    }

    @PostMapping("/{tableId}/book")
    public ResponseEntity<ReservationResponse> bookTable(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID tableId,
            @Valid @RequestBody BookTableRequest request) {
        Reservation reservation = reservationService.bookNow(tableId, principal.restaurantId(), request, principal.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservationResponse.from(reservation));
    }
}
