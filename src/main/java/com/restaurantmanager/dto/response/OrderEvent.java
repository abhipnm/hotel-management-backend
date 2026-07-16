package com.restaurantmanager.dto.response;

/** Broadcast over the /topic/restaurants/{restaurantId}/orders websocket topic. */
public record OrderEvent(
        Type type,
        OrderResponse order
) {
    public enum Type {
        ORDER_CREATED,
        ORDER_STATUS_CHANGED
    }

    public static OrderEvent created(OrderResponse order) {
        return new OrderEvent(Type.ORDER_CREATED, order);
    }

    public static OrderEvent statusChanged(OrderResponse order) {
        return new OrderEvent(Type.ORDER_STATUS_CHANGED, order);
    }
}
