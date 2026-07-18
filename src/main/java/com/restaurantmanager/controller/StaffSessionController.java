package com.restaurantmanager.controller;

import com.restaurantmanager.dto.response.BillResponse;
import com.restaurantmanager.dto.response.StaffGuestSessionResponse;
import com.restaurantmanager.security.AuthPrincipal;
import com.restaurantmanager.service.GuestSessionService;
import com.restaurantmanager.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Floor-service view of currently active guest sessions (e.g. tables awaiting payment). */
@Tag(name = "Staff", description = "Active guest sessions and bill settlement")
@RestController
@RequestMapping("/api/v1/staff/sessions")
@RequiredArgsConstructor
public class StaffSessionController {

    private final GuestSessionService guestSessionService;
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<StaffGuestSessionResponse>> listActiveSessions(@AuthenticationPrincipal AuthPrincipal principal) {
        List<StaffGuestSessionResponse> sessions = guestSessionService.listActiveSessions(principal.restaurantId()).stream()
                .map(session -> StaffGuestSessionResponse.from(session, guestSessionService.getVisitCount(session)))
                .collect(Collectors.toList());
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{sessionId}/bill")
    public ResponseEntity<BillResponse> getBill(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID sessionId) {
        var session = guestSessionService.getForRestaurant(sessionId, principal.restaurantId());
        var orders = orderService.listForGuestSession(sessionId);
        return ResponseEntity.ok(BillResponse.from(session, orders));
    }

    @PatchMapping("/{sessionId}/mark-paid")
    public ResponseEntity<StaffGuestSessionResponse> markPaid(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID sessionId) {
        var session = guestSessionService.markPaid(sessionId, principal.restaurantId());
        return ResponseEntity.ok(StaffGuestSessionResponse.from(session, guestSessionService.getVisitCount(session)));
    }

    @PatchMapping("/{sessionId}/close")
    public ResponseEntity<Void> closeTable(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID sessionId) {
        guestSessionService.closeSession(sessionId, principal.restaurantId());
        return ResponseEntity.noContent().build();
    }
}
