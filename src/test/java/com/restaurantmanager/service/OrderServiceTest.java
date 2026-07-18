package com.restaurantmanager.service;

import com.restaurantmanager.dto.request.OrderItemRequest;
import com.restaurantmanager.dto.request.PlaceOrderRequest;
import com.restaurantmanager.entity.AppUser;
import com.restaurantmanager.entity.GuestSession;
import com.restaurantmanager.entity.GuestSessionStatus;
import com.restaurantmanager.entity.MenuItem;
import com.restaurantmanager.entity.Order;
import com.restaurantmanager.entity.OrderStatus;
import com.restaurantmanager.entity.Restaurant;
import com.restaurantmanager.entity.RestaurantTable;
import com.restaurantmanager.entity.Role;
import com.restaurantmanager.exception.BadRequestException;
import com.restaurantmanager.exception.InvalidOrderStateException;
import com.restaurantmanager.repository.AppUserRepository;
import com.restaurantmanager.repository.MenuItemRepository;
import com.restaurantmanager.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private OrderBroadcastService broadcastService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderService orderService;

    private Restaurant restaurant;
    private RestaurantTable table;
    private GuestSession session;
    private MenuItem burger;
    private MenuItem fries;

    @BeforeEach
    void setUp() {
        restaurant = Restaurant.builder().name("Test Diner").slug("test-diner").active(true).build();
        restaurant.setId(UUID.randomUUID());

        table = RestaurantTable.builder().restaurant(restaurant).tableNumber("12").qrToken(UUID.randomUUID().toString()).active(true).build();
        table.setId(UUID.randomUUID());

        session = GuestSession.builder()
                .restaurant(restaurant)
                .table(table)
                .guestName("Priya")
                .status(GuestSessionStatus.ACTIVE)
                .expiresAt(Instant.now().plus(2, ChronoUnit.HOURS))
                .build();
        session.setId(UUID.randomUUID());

        burger = MenuItem.builder().restaurant(restaurant).name("Cheeseburger").price(new BigDecimal("9.50")).available(true).build();
        burger.setId(UUID.randomUUID());

        fries = MenuItem.builder().restaurant(restaurant).name("Fries").price(new BigDecimal("3.00")).available(false).build();
        fries.setId(UUID.randomUUID());
    }

    @Test
    void placeOrder_computesTotalFromLineItems() {
        when(menuItemRepository.findByIdAndRestaurantId(burger.getId(), restaurant.getId()))
                .thenReturn(Optional.of(burger));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(UUID.randomUUID());
            }
            return order;
        });

        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(burger.getId(), 2, "no onions")),
                "Table by the window",
                null
        );

        Order order = orderService.placeOrder(session, request);

        assertThat(order.getTotalAmount()).isEqualByComparingTo("19.00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getSubtotal()).isEqualByComparingTo("19.00");
        verify(broadcastService).broadcastCreated(order);
    }

    @Test
    void placeOrder_rejectsUnavailableItems() {
        when(menuItemRepository.findByIdAndRestaurantId(fries.getId(), restaurant.getId()))
                .thenReturn(Optional.of(fries));

        PlaceOrderRequest request = new PlaceOrderRequest(
                List.of(new OrderItemRequest(fries.getId(), 1, null)),
                null,
                null
        );

        assertThatThrownBy(() -> orderService.placeOrder(session, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("unavailable");
    }

    @Test
    void updateStatus_allowsValidTransition() {
        Order order = Order.builder()
                .restaurant(restaurant).table(table).guestSession(session)
                .status(OrderStatus.PLACED).totalAmount(BigDecimal.TEN).build();
        order.setId(UUID.randomUUID());

        when(orderRepository.findByIdAndRestaurantId(order.getId(), restaurant.getId()))
                .thenReturn(Optional.of(order));

        Order updated = orderService.updateStatus(order.getId(), restaurant.getId(), OrderStatus.ACCEPTED, UUID.randomUUID());

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        verify(broadcastService).broadcastStatusChanged(order);
    }

    @Test
    void updateStatus_servedStampsServedAtAndServedBy() {
        Order order = Order.builder()
                .restaurant(restaurant).table(table).guestSession(session)
                .status(OrderStatus.READY).totalAmount(BigDecimal.TEN).build();
        order.setId(UUID.randomUUID());

        AppUser waiter = AppUser.builder().restaurant(restaurant).name("Kiran").email("kiran@test.diner")
                .passwordHash("hash").role(Role.STAFF).build();
        waiter.setId(UUID.randomUUID());

        when(orderRepository.findByIdAndRestaurantId(order.getId(), restaurant.getId()))
                .thenReturn(Optional.of(order));
        when(appUserRepository.getReferenceById(waiter.getId())).thenReturn(waiter);

        Order updated = orderService.updateStatus(order.getId(), restaurant.getId(), OrderStatus.SERVED, waiter.getId());

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.SERVED);
        assertThat(updated.getServedAt()).isNotNull();
        assertThat(updated.getServedBy()).isEqualTo(waiter);
    }

    @Test
    void updateStatus_rejectsInvalidTransition() {
        Order order = Order.builder()
                .restaurant(restaurant).table(table).guestSession(session)
                .status(OrderStatus.SERVED).totalAmount(BigDecimal.TEN).build();
        order.setId(UUID.randomUUID());

        when(orderRepository.findByIdAndRestaurantId(order.getId(), restaurant.getId()))
                .thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(order.getId(), restaurant.getId(), OrderStatus.PREPARING, UUID.randomUUID()))
                .isInstanceOf(InvalidOrderStateException.class);
    }
}
