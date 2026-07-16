package com.restaurantmanager.service;

import com.restaurantmanager.dto.response.NotificationResponse;
import com.restaurantmanager.entity.GuestSession;
import com.restaurantmanager.entity.MenuItem;
import com.restaurantmanager.entity.Notification;
import com.restaurantmanager.entity.NotificationAudience;
import com.restaurantmanager.entity.NotificationType;
import com.restaurantmanager.entity.Order;
import com.restaurantmanager.entity.Restaurant;
import com.restaurantmanager.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Persisted, restaurant-scoped notifications. STAFF-audience rows also push live over websocket. */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void notifyNewOrder(Order order) {
        createForStaff(order.getRestaurant(), NotificationType.NEW_ORDER,
                "New order",
                "Table " + order.getTable().getTableNumber() + " placed a new order");
    }

    @Transactional
    public void notifyOrderReady(Order order) {
        createForGuest(order.getRestaurant(), order.getGuestSession(), NotificationType.ORDER_READY,
                "Your order is ready",
                "Your order at table " + order.getTable().getTableNumber() + " is ready to be served");
    }

    @Transactional
    public void notifyPaymentSuccess(GuestSession session) {
        createForGuest(session.getRestaurant(), session, NotificationType.PAYMENT_SUCCESS,
                "Payment received",
                "Your bill for table " + session.getTable().getTableNumber() + " has been settled. Thank you!");
    }

    @Transactional
    public void notifyLowInventory(MenuItem item) {
        createForStaff(item.getRestaurant(), NotificationType.LOW_INVENTORY,
                "Low inventory",
                "'" + item.getName() + "' is running low (" + item.getStockQuantity() + " left)");
    }

    @Transactional
    public void notifyCallWaiter(GuestSession session) {
        createForStaff(session.getRestaurant(), NotificationType.CALL_WAITER,
                "Waiter called",
                "Table " + session.getTable().getTableNumber() + " (" + session.getGuestName() + ") called for a waiter");
    }

    @Transactional
    public void notifyBillRequested(GuestSession session) {
        createForStaff(session.getRestaurant(), NotificationType.BILL_REQUESTED,
                "Bill requested",
                "Table " + session.getTable().getTableNumber() + " (" + session.getGuestName() + ") requested the bill");
    }

    @Transactional
    public void createAnnouncement(Restaurant restaurant, String message) {
        createForStaff(restaurant, NotificationType.ANNOUNCEMENT, "Announcement", message);
        create(restaurant, null, NotificationAudience.GUEST, NotificationType.ANNOUNCEMENT, "Announcement", message);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForStaff(UUID restaurantId) {
        return notificationRepository.findByRestaurantIdAndAudienceOrderByCreatedAtDesc(restaurantId, NotificationAudience.STAFF)
                .stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCountForStaff(UUID restaurantId) {
        return notificationRepository.countByRestaurantIdAndAudienceAndReadFalse(restaurantId, NotificationAudience.STAFF);
    }

    @Transactional
    public void markAllStaffRead(UUID restaurantId) {
        notificationRepository.markAllStaffRead(restaurantId);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForGuestSession(UUID restaurantId, UUID guestSessionId) {
        return notificationRepository.findForGuestSession(restaurantId, guestSessionId)
                .stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCountForGuestSession(UUID restaurantId, UUID guestSessionId) {
        return notificationRepository.countUnreadForGuestSession(restaurantId, guestSessionId);
    }

    @Transactional
    public void markAllGuestRead(UUID restaurantId, UUID guestSessionId) {
        notificationRepository.markAllGuestRead(restaurantId, guestSessionId);
    }

    private void createForStaff(Restaurant restaurant, NotificationType type, String title, String message) {
        Notification saved = create(restaurant, null, NotificationAudience.STAFF, type, title, message);
        try {
            String destination = "/topic/restaurants/" + restaurant.getId() + "/notifications";
            messagingTemplate.convertAndSend(destination, NotificationResponse.from(saved));
        } catch (Exception e) {
            // A broadcast failure must never fail the underlying business transaction.
            log.warn("Failed to broadcast notification {}: {}", saved.getId(), e.getMessage());
        }
    }

    private void createForGuest(Restaurant restaurant, GuestSession session, NotificationType type, String title, String message) {
        create(restaurant, session, NotificationAudience.GUEST, type, title, message);
    }

    private Notification create(
            Restaurant restaurant, GuestSession session, NotificationAudience audience,
            NotificationType type, String title, String message) {
        Notification notification = Notification.builder()
                .restaurant(restaurant)
                .guestSession(session)
                .audience(audience)
                .type(type)
                .title(title)
                .message(message)
                .read(false)
                .build();
        return notificationRepository.save(notification);
    }
}
