package com.restaurantmanager.service;

import com.restaurantmanager.dto.response.StaffAlertEvent;
import com.restaurantmanager.entity.GuestSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/** Ephemeral floor-service notifications (call waiter, bill requested) pushed to the staff dashboard. */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffAlertService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public void broadcastCallWaiter(GuestSession session) {
        broadcast(session, StaffAlertEvent.callWaiter(session));
        notificationService.notifyCallWaiter(session);
    }

    public void broadcastBillRequested(GuestSession session) {
        broadcast(session, StaffAlertEvent.billRequested(session));
        notificationService.notifyBillRequested(session);
    }

    private void broadcast(GuestSession session, StaffAlertEvent event) {
        try {
            String destination = "/topic/restaurants/" + session.getRestaurant().getId() + "/alerts";
            messagingTemplate.convertAndSend(destination, event);
        } catch (Exception e) {
            // A broadcast failure must never fail the underlying business transaction.
            log.warn("Failed to broadcast staff alert for session {}: {}", session.getId(), e.getMessage());
        }
    }
}
