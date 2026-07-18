package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.ActivityLog;

import java.time.Instant;
import java.util.UUID;

public record ActivityLogResponse(
        UUID id,
        String actorName,
        String action,
        String description,
        Instant createdAt
) {
    public static ActivityLogResponse from(ActivityLog entry) {
        return new ActivityLogResponse(
                entry.getId(),
                entry.getActorName(),
                entry.getAction(),
                entry.getDescription(),
                entry.getCreatedAt()
        );
    }
}
