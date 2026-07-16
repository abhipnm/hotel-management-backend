package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.Feedback;

import java.time.Instant;
import java.util.UUID;

public record FeedbackResponse(
        UUID id,
        int rating,
        String comment,
        String guestName,
        String tableNumber,
        Instant createdAt
) {
    public static FeedbackResponse from(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getRating(),
                feedback.getComment(),
                feedback.getGuestNameSnapshot(),
                feedback.getTableNumberSnapshot(),
                feedback.getCreatedAt()
        );
    }
}
