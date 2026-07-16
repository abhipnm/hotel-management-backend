package com.restaurantmanager.service;

import com.restaurantmanager.dto.response.OrderEvent;
import com.restaurantmanager.dto.response.OrderResponse;
import com.restaurantmanager.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastCreated(Order order) {
        broadcast(order, OrderEvent::created);
    }

    public void broadcastStatusChanged(Order order) {
        broadcast(order, OrderEvent::statusChanged);
    }

    private void broadcast(Order order, java.util.function.Function<OrderResponse, OrderEvent> eventFactory) {
        try {
            String destination = "/topic/restaurants/" + order.getRestaurant().getId() + "/orders";
            messagingTemplate.convertAndSend(destination, eventFactory.apply(OrderResponse.from(order)));
        } catch (Exception e) {
            // A broadcast failure must never fail the underlying business transaction.
            log.warn("Failed to broadcast order event for order {}: {}", order.getId(), e.getMessage());
        }
    }
}
