package com.restaurantmanager.service;

import com.restaurantmanager.dto.request.CreateFeedbackRequest;
import com.restaurantmanager.dto.response.FeedbackResponse;
import com.restaurantmanager.dto.response.FeedbackSummaryResponse;
import com.restaurantmanager.entity.Feedback;
import com.restaurantmanager.entity.GuestSession;
import com.restaurantmanager.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    /**
     * Records feedback for a guest session. One entry exists per visit, so a
     * re-submit updates the existing row rather than creating a duplicate.
     */
    @Transactional
    public Feedback submit(GuestSession session, CreateFeedbackRequest request) {
        Feedback feedback = feedbackRepository.findByGuestSessionId(session.getId())
                .orElseGet(() -> Feedback.builder()
                        .restaurant(session.getRestaurant())
                        .guestSession(session)
                        .guestNameSnapshot(session.getGuestName())
                        .tableNumberSnapshot(session.getTable().getTableNumber())
                        .build());

        feedback.setRating(request.rating());
        feedback.setComment(request.comment() != null && !request.comment().isBlank() ? request.comment().trim() : null);
        Feedback saved = feedbackRepository.save(feedback);
        log.debug("Feedback {} recorded for session {} (rating {})", saved.getId(), session.getId(), saved.getRating());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Feedback> findForSession(UUID guestSessionId) {
        return feedbackRepository.findByGuestSessionId(guestSessionId);
    }

    @Transactional(readOnly = true)
    public FeedbackSummaryResponse getSummary(UUID restaurantId) {
        List<FeedbackResponse> items = feedbackRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId).stream()
                .map(FeedbackResponse::from)
                .collect(Collectors.toList());
        double average = BigDecimal.valueOf(feedbackRepository.averageRating(restaurantId))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        return new FeedbackSummaryResponse(feedbackRepository.countByRestaurantId(restaurantId), average, items);
    }
}
