package com.restaurantmanager.dto.response;

import java.util.List;

/** Admin-facing feedback overview: headline stats plus the individual entries. */
public record FeedbackSummaryResponse(
        long count,
        double averageRating,
        List<FeedbackResponse> items
) {
}
