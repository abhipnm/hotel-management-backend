package com.restaurantmanager.dto.request;

import java.util.UUID;

/** waiterId is nullable — pass null to unassign the table. */
public record AssignWaiterRequest(UUID waiterId) {
}
