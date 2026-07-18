package com.restaurantmanager.entity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    SEATED,
    CANCELLED,
    NO_SHOW;

    private static final Map<ReservationStatus, Set<ReservationStatus>> ALLOWED_TRANSITIONS = Map.of(
            PENDING, EnumSet.of(CONFIRMED, CANCELLED),
            CONFIRMED, EnumSet.of(SEATED, CANCELLED, NO_SHOW),
            SEATED, EnumSet.noneOf(ReservationStatus.class),
            CANCELLED, EnumSet.noneOf(ReservationStatus.class),
            NO_SHOW, EnumSet.noneOf(ReservationStatus.class)
    );

    public boolean canTransitionTo(ReservationStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
