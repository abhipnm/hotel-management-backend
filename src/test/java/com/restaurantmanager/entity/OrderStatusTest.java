package com.restaurantmanager.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void placedCanMoveToAcceptedOrCancelled() {
        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.ACCEPTED)).isTrue();
        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.PREPARING)).isFalse();
        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.READY)).isFalse();
        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.SERVED)).isFalse();
    }

    @Test
    void fullHappyPathIsLinear() {
        assertThat(OrderStatus.ACCEPTED.canTransitionTo(OrderStatus.PREPARING)).isTrue();
        assertThat(OrderStatus.PREPARING.canTransitionTo(OrderStatus.READY)).isTrue();
        assertThat(OrderStatus.READY.canTransitionTo(OrderStatus.SERVED)).isTrue();
    }

    @Test
    void readyCanNoLongerBeCancelled() {
        assertThat(OrderStatus.READY.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"SERVED", "CANCELLED"})
    void terminalStatusesHaveNoValidTransitions(OrderStatus terminal) {
        for (OrderStatus target : OrderStatus.values()) {
            assertThat(terminal.canTransitionTo(target)).isFalse();
        }
    }
}
