package com.restaurantmanager.entity;

/**
 * Staff-side roles. ADMIN can manage the restaurant profile, tables, menu and staff.
 * STAFF (kitchen/waiter) can view and progress orders only.
 */
public enum Role {
    ADMIN,
    STAFF
}
