package com.restaurantmanager.controller;

import com.restaurantmanager.dto.request.AssignWaiterRequest;
import com.restaurantmanager.dto.request.CreateTableRequest;
import com.restaurantmanager.dto.request.SetTableActiveRequest;
import com.restaurantmanager.dto.request.UpdateTableRequest;
import com.restaurantmanager.dto.response.TableResponse;
import com.restaurantmanager.entity.GuestSession;
import com.restaurantmanager.entity.Restaurant;
import com.restaurantmanager.entity.RestaurantTable;
import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.service.GuestSessionService;
import com.restaurantmanager.service.RestaurantService;
import com.restaurantmanager.service.TableService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Admin - Tables", description = "Manage tables and their QR codes")
@RestController
@RequestMapping("/api/v1/admin/tables")
@RequiredArgsConstructor
public class AdminTableController {

    private final TableService tableService;
    private final RestaurantService restaurantService;
    private final GuestSessionService guestSessionService;

    @GetMapping
    public ResponseEntity<List<TableResponse>> listTables(@AuthenticationPrincipal AuthPrincipal principal) {
        // One query for the occupancy set, reused per table below — avoids N+1.
        Set<UUID> occupiedTableIds = guestSessionService.listActiveSessions(principal.restaurantId()).stream()
                .map(GuestSession::getTable)
                .map(RestaurantTable::getId)
                .collect(Collectors.toSet());

        List<TableResponse> tables = tableService.listForRestaurant(principal.restaurantId()).stream()
                .map(table -> TableResponse.from(table, occupiedTableIds.contains(table.getId())))
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

    @PatchMapping("/{tableId}/active")
    public ResponseEntity<TableResponse> setActive(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID tableId,
            @Valid @RequestBody SetTableActiveRequest request) {
        RestaurantTable table = tableService.setActive(tableId, principal.restaurantId(), request.active(), principal.id());
        return ResponseEntity.ok(TableResponse.from(table));
    }

    @PutMapping("/{tableId}")
    public ResponseEntity<TableResponse> updateTable(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID tableId,
            @Valid @RequestBody UpdateTableRequest request) {
        RestaurantTable table = tableService.update(tableId, principal.restaurantId(), request);
        return ResponseEntity.ok(TableResponse.from(table));
    }

    @PatchMapping("/{tableId}/waiter")
    public ResponseEntity<TableResponse> assignWaiter(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID tableId,
            @RequestBody AssignWaiterRequest request) {
        RestaurantTable table = tableService.assignWaiter(tableId, principal.restaurantId(), request.waiterId(), principal.id());
        return ResponseEntity.ok(TableResponse.from(table));
    }

    @DeleteMapping("/{tableId}")
    public ResponseEntity<Void> deleteTable(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID tableId) {
        tableService.delete(tableId, principal.restaurantId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{tableId}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrCode(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID tableId) {
        RestaurantTable table = tableService.getForRestaurant(tableId, principal.restaurantId());
        byte[] png = tableService.generateQrPng(table);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }
}
