package com.restaurantmanager.controller;

import com.restaurantmanager.dto.response.StaffTableGuestResponse;
import com.restaurantmanager.dto.response.StaffTableResponse;
import com.restaurantmanager.entity.GuestSession;
import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.service.GuestSessionService;
import com.restaurantmanager.service.TableService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Read-only floor view for waitstaff: every table with its live occupancy, guests and bill-requested flag. */
@Tag(name = "Staff", description = "Floor view of tables")
@RestController
@RequestMapping("/api/v1/staff/tables")
@RequiredArgsConstructor
public class StaffTableController {

    private final TableService tableService;
    private final GuestSessionService guestSessionService;

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
}
