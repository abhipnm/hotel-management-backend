package com.restaurantmanager.service;

import com.restaurantmanager.dto.request.CreateGuestSessionRequest;
import com.restaurantmanager.dto.response.GuestSessionResponse;
import com.restaurantmanager.entity.GuestSession;
import com.restaurantmanager.entity.GuestSessionStatus;
import com.restaurantmanager.entity.RestaurantTable;
import com.restaurantmanager.exception.BadRequestException;
import com.restaurantmanager.exception.ResourceNotFoundException;
import com.restaurantmanager.repository.GuestSessionRepository;
import com.restaurantmanager.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuestSessionService {

    private final GuestSessionRepository guestSessionRepository;
    private final TableService tableService;
    private final JwtService jwtService;
    private final StaffAlertService staffAlertService;
    private final NotificationService notificationService;

    @Value("${app.jwt.guest-ttl-minutes:240}")
    private long guestTtlMinutes;

    @Transactional
    public GuestSessionResponse createSession(CreateGuestSessionRequest request) {
        RestaurantTable table = tableService.getByQrToken(request.qrToken());

        GuestSession session = GuestSession.builder()
                .restaurant(table.getRestaurant())
                .table(table)
                .guestName(request.guestName())
                .guestPhone(request.guestPhone())
                .status(GuestSessionStatus.ACTIVE)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(guestTtlMinutes)))
                .build();
        session = guestSessionRepository.save(session);

        JwtService.IssuedToken issued = jwtService.generateGuestToken(session);

        return new GuestSessionResponse(
                session.getId(),
                issued.token(),
                "Bearer",
                issued.expiresAt(),
                session.getGuestName(),
                table.getRestaurant().getName(),
                table.getTableNumber(),
                getVisitCount(session)
        );
    }

    /** Null when the guest didn't give a phone number; otherwise total visits by that phone at this restaurant, including this one. */
    @Transactional(readOnly = true)
    public Integer getVisitCount(GuestSession session) {
        if (session.getGuestPhone() == null || session.getGuestPhone().isBlank()) {
            return null;
        }
        return (int) guestSessionRepository.countByRestaurantIdAndGuestPhone(session.getRestaurant().getId(), session.getGuestPhone());
    }

    @Transactional(readOnly = true)
    public GuestSession requireActiveSession(UUID sessionId, UUID restaurantId) {
        GuestSession session = guestSessionRepository.findByIdAndRestaurantId(sessionId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Guest session not found"));

        if (session.getStatus() != GuestSessionStatus.ACTIVE) {
            throw new BadRequestException("This session has been closed. Please scan the QR code again.");
        }
        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("This session has expired. Please scan the QR code again.");
        }
        return session;
    }

    @Transactional
    public void closeSession(UUID sessionId, UUID restaurantId) {
        GuestSession session = requireActiveSession(sessionId, restaurantId);
        session.setStatus(GuestSessionStatus.CLOSED);
    }

    /** Looks up a session for staff/admin views, regardless of its current status (active, paid, or closed). */
    @Transactional(readOnly = true)
    public GuestSession getForRestaurant(UUID sessionId, UUID restaurantId) {
        return guestSessionRepository.findByIdAndRestaurantId(sessionId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Guest session not found"));
    }

    @Transactional
    public GuestSession requestBill(UUID sessionId, UUID restaurantId) {
        GuestSession session = requireActiveSession(sessionId, restaurantId);
        if (!session.isBillRequested()) {
            session.setBillRequested(true);
            staffAlertService.broadcastBillRequested(session);
        }
        return session;
    }

    @Transactional(readOnly = true)
    public List<GuestSession> listActiveSessions(UUID restaurantId) {
        return guestSessionRepository.findByRestaurantIdAndStatusOrderByCreatedAtAsc(restaurantId, GuestSessionStatus.ACTIVE);
    }

    @Transactional
    public GuestSession markPaid(UUID sessionId, UUID restaurantId) {
        GuestSession session = guestSessionRepository.findByIdAndRestaurantId(sessionId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Guest session not found"));
        if (session.getPaidAt() == null) {
            session.setPaidAt(Instant.now());
            notificationService.notifyPaymentSuccess(session);
        }
        session.setStatus(GuestSessionStatus.CLOSED);
        return session;
    }
}
