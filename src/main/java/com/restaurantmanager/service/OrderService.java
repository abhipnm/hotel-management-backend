package com.restaurantmanager.service;

import com.restaurantmanager.dto.request.OrderItemRequest;
import com.restaurantmanager.dto.request.PlaceOrderRequest;
import com.restaurantmanager.entity.Coupon;
import com.restaurantmanager.entity.GuestSession;
import com.restaurantmanager.entity.MenuItem;
import com.restaurantmanager.entity.Order;
import com.restaurantmanager.entity.OrderItem;
import com.restaurantmanager.entity.OrderStatus;
import com.restaurantmanager.exception.BadRequestException;
import com.restaurantmanager.exception.InvalidOrderStateException;
import com.restaurantmanager.exception.ResourceNotFoundException;
import com.restaurantmanager.repository.AppUserRepository;
import com.restaurantmanager.repository.MenuItemRepository;
import com.restaurantmanager.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderBroadcastService broadcastService;
    private final AppUserRepository appUserRepository;
    private final NotificationService notificationService;
    private final CouponService couponService;
    private final ActivityLogService activityLogService;

    @Transactional
    public Order placeOrder(GuestSession session, PlaceOrderRequest request) {
        Order order = Order.builder()
                .restaurant(session.getRestaurant())
                .table(session.getTable())
                .guestSession(session)
                .status(OrderStatus.PLACED)
                .notes(request.notes())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.items()) {
            MenuItem menuItem = menuItemRepository.findByIdAndRestaurantId(itemRequest.menuItemId(), session.getRestaurant().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + itemRequest.menuItemId()));

            if (!menuItem.isAvailable()) {
                throw new BadRequestException("'" + menuItem.getName() + "' is currently unavailable");
            }

            BigDecimal lineTotal = menuItem.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity()));
            OrderItem orderItem = OrderItem.builder()
                    .menuItem(menuItem)
                    .itemNameSnapshot(menuItem.getName())
                    .itemPriceSnapshot(menuItem.getPrice())
                    .quantity(itemRequest.quantity())
                    .subtotal(lineTotal)
                    .notes(itemRequest.notes())
                    .build();

            order.addItem(orderItem);
            subtotal = subtotal.add(lineTotal);
            decrementStock(menuItem, itemRequest.quantity());
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            Coupon coupon = couponService.findValid(session.getRestaurant().getId(), request.couponCode(), subtotal);
            discountAmount = couponService.calculateDiscount(coupon, subtotal);
            couponService.recordUsage(coupon);
            order.setCouponCode(coupon.getCode());
        }

        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(subtotal.subtract(discountAmount));
        Order saved = orderRepository.save(order);
        broadcastService.broadcastCreated(saved);
        notificationService.notifyNewOrder(saved);
        return saved;
    }

    /** Only tracks stock for items that have it set; untracked items (null stockQuantity) are unaffected. */
    private void decrementStock(MenuItem menuItem, int quantityOrdered) {
        Integer before = menuItem.getStockQuantity();
        if (before == null) {
            return;
        }
        Integer threshold = menuItem.getLowStockThreshold();
        int after = Math.max(before - quantityOrdered, 0);
        menuItem.setStockQuantity(after);
        if (after == 0) {
            menuItem.setAvailable(false);
        }
        // Fire only on the crossing (was above threshold, now at or below it) to avoid repeat alerts on every order.
        if (threshold != null && before > threshold && after <= threshold) {
            notificationService.notifyLowInventory(menuItem);
        }
    }

    @Transactional(readOnly = true)
    public List<Order> listForGuestSession(UUID guestSessionId) {
        return orderRepository.findByGuestSessionIdOrderByCreatedAtDesc(guestSessionId);
    }

    @Transactional(readOnly = true)
    public Order getForGuestSession(UUID orderId, UUID guestSessionId) {
        return orderRepository.findByIdAndGuestSessionId(orderId, guestSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    @Transactional(readOnly = true)
    public List<Order> listForRestaurant(UUID restaurantId, Collection<OrderStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return orderRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId);
        }
        return orderRepository.findByRestaurantIdAndStatusInOrderByCreatedAtAsc(restaurantId, statuses);
    }

    @Transactional(readOnly = true)
    public Order getForRestaurant(UUID orderId, UUID restaurantId) {
        return orderRepository.findByIdAndRestaurantId(orderId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    @Transactional
    public Order updateStatus(UUID orderId, UUID restaurantId, OrderStatus targetStatus, UUID actingUserId) {
        Order order = getForRestaurant(orderId, restaurantId);

        if (!order.getStatus().canTransitionTo(targetStatus)) {
            throw new InvalidOrderStateException(
                    "Cannot move order from " + order.getStatus() + " to " + targetStatus);
        }

        order.setStatus(targetStatus);
        if (targetStatus == OrderStatus.ACCEPTED) {
            order.setAcceptedAt(Instant.now());
        } else if (targetStatus == OrderStatus.READY) {
            notificationService.notifyOrderReady(order);
        } else if (targetStatus == OrderStatus.SERVED) {
            order.setServedAt(Instant.now());
            order.setServedBy(appUserRepository.getReferenceById(actingUserId));
        } else if (targetStatus == OrderStatus.CANCELLED) {
            restoreStock(order);
            activityLogService.log(restaurantId, actingUserId, "ORDER_CANCELLED",
                    "Cancelled order for Table " + order.getTable().getTableNumber() + " (" + order.getTotalAmount() + ")");
        }
        broadcastService.broadcastStatusChanged(order);
        return order;
    }

    /** Gives back whatever stock was decremented at placement time. Only affects items that track stock. */
    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            MenuItem menuItem = item.getMenuItem();
            Integer current = menuItem.getStockQuantity();
            if (current == null) {
                continue;
            }
            menuItem.setStockQuantity(current + item.getQuantity());
        }
    }
}
