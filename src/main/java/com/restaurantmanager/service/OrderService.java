package com.restaurantmanager.service;

import com.restaurantmanager.dto.request.OrderItemRequest;
import com.restaurantmanager.dto.request.PlaceOrderRequest;
import com.restaurantmanager.dto.request.UpdateOrderItemsRequest;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    /** Once the kitchen has accepted an order, its contents are considered locked. */
    private static final Set<OrderStatus> EDITABLE_STATUSES = EnumSet.of(OrderStatus.PLACED);

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
            assertStockAvailable(menuItem, itemRequest.quantity());

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

    /** Untracked items (null stockQuantity) have unlimited availability and are never rejected here. */
    private void assertStockAvailable(MenuItem menuItem, int quantityRequested) {
        Integer available = menuItem.getStockQuantity();
        if (available != null && quantityRequested > available) {
            throw new BadRequestException(
                    "Only " + available + " of '" + menuItem.getName() + "' left in stock");
        }
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

    /**
     * Lets staff/admin correct an order's contents (e.g. the guest asks for one more naan, or to drop an item)
     * without cancelling and re-placing it. Rebuilds the item list from scratch: gives back the stock the
     * original items held, then re-decrements for the new list, so stock accounting stays correct either way.
     */
    @Transactional
    public Order updateItems(UUID orderId, UUID restaurantId, UpdateOrderItemsRequest request, UUID actorId) {
        Order order = getForRestaurant(orderId, restaurantId);

        if (!EDITABLE_STATUSES.contains(order.getStatus())) {
            throw new InvalidOrderStateException("Cannot modify an order that is " + order.getStatus());
        }

        restoreStock(order);
        order.getItems().clear();

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.items()) {
            MenuItem menuItem = menuItemRepository.findByIdAndRestaurantId(itemRequest.menuItemId(), restaurantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + itemRequest.menuItemId()));

            if (!menuItem.isAvailable()) {
                throw new BadRequestException("'" + menuItem.getName() + "' is currently unavailable");
            }
            assertStockAvailable(menuItem, itemRequest.quantity());

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
        if (order.getCouponCode() != null) {
            try {
                Coupon coupon = couponService.findValid(restaurantId, order.getCouponCode(), subtotal);
                discountAmount = couponService.calculateDiscount(coupon, subtotal);
            } catch (BadRequestException e) {
                // The coupon no longer applies to the new subtotal (e.g. now below its minimum) — drop the
                // discount rather than block the edit; the order simply reverts to full price.
                discountAmount = BigDecimal.ZERO;
            }
        }

        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(subtotal.subtract(discountAmount));
        if (request.notes() != null) {
            order.setNotes(request.notes());
        }

        activityLogService.log(restaurantId, actorId, "ORDER_MODIFIED",
                "Modified order for Table " + order.getTable().getTableNumber());
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
