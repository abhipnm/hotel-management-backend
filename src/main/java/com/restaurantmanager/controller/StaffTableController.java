package com.restaurantmanager.controller;

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
import java.util.function.Function;
import java.util.stream.Collectors;

/** Read-only floor view for waitstaff: every table with its live occupancy, guest and bill-requested flag. */
@Tag(name = "Staff", description = "Floor view of tables")
@RestController
@RequestMapping("/api/v1/staff/tables")
@RequiredArgsConstructor
public class StaffTableController {

    private final TableService tableService;
    private final GuestSessionService guestSessionService;

    @GetMapping
    public ResponseEntity<List<StaffTableResponse>> listTables(@AuthenticationPrincipal AuthPrincipal principal) {
        Map<UUID, GuestSession> sessionsByTableId = guestSessionService.listActiveSessions(principal.restaurantId()).stream()
                .collect(Collectors.toMap(s -> s.getTable().getId(), Function.identity(), (a, b) -> a));

        List<StaffTableResponse> tables = tableService.listForRestaurant(principal.restaurantId()).stream()
                .map(table -> StaffTableResponse.from(table, sessionsByTableId.get(table.getId())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(tables);
    }
}
