package com.restaurantmanager.service;

import com.restaurantmanager.dto.request.BookTableRequest;
import com.restaurantmanager.dto.request.CreateReservationRequest;
import com.restaurantmanager.entity.Reservation;
import com.restaurantmanager.entity.ReservationStatus;
import com.restaurantmanager.entity.Restaurant;
import com.restaurantmanager.entity.RestaurantTable;
import com.restaurantmanager.exception.BadRequestException;
import com.restaurantmanager.exception.ConflictException;
import com.restaurantmanager.exception.ResourceNotFoundException;
import com.restaurantmanager.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    /** How long a table is considered held by a reservation, for conflict purposes. */
    private static final Duration RESERVATION_WINDOW = Duration.ofMinutes(90);
    private static final Collection<ReservationStatus> TABLE_HOLDING_STATUSES =
            EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.SEATED);

    private final ReservationRepository reservationRepository;
    private final TableService tableService;
    private final ActivityLogService activityLogService;

    @Transactional
    public Reservation create(Restaurant restaurant, CreateReservationRequest request) {
        RestaurantTable table = request.tableId() != null
                ? tableService.getForRestaurant(request.tableId(), restaurant.getId())
                : null;

        if (table != null) {
            requireNoConflict(table.getId(), request.reservationTime(), null);
        }

        Reservation reservation = Reservation.builder()
                .restaurant(restaurant)
                .table(table)
                .guestName(request.guestName())
                .guestPhone(request.guestPhone())
                .partySize(request.partySize())
                .reservationTime(request.reservationTime())
                .notes(request.notes())
                .status(ReservationStatus.PENDING)
                .build();
        return reservationRepository.save(reservation);
    }

    /** A walk-in booking made right now for a specific table — skips the future-time requirement of a regular reservation. */
    @Transactional
    public Reservation bookNow(UUID tableId, UUID restaurantId, BookTableRequest request, UUID actorId) {
        RestaurantTable table = tableService.getForRestaurant(tableId, restaurantId);
        if (!table.isActive()) {
            throw new BadRequestException("Table " + table.getTableNumber() + " is blocked and cannot be booked");
        }
        Instant now = Instant.now();
        requireNoConflict(table.getId(), now, null);

        Reservation reservation = Reservation.builder()
                .restaurant(table.getRestaurant())
                .table(table)
                .guestName(request.guestName())
                .guestPhone(request.guestPhone())
                .partySize(request.partySize())
                .reservationTime(now)
                .status(ReservationStatus.CONFIRMED)
                .build();
        reservation = reservationRepository.save(reservation);

        activityLogService.log(restaurantId, actorId, "TABLE_BOOKED",
                "Booked Table " + table.getTableNumber() + " for " + request.guestName()
                        + " (party of " + request.partySize() + ")");
        return reservation;
    }

    @Transactional(readOnly = true)
    public List<Reservation> listForRestaurant(UUID restaurantId, Collection<ReservationStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return reservationRepository.findByRestaurantIdOrderByReservationTimeAsc(restaurantId);
        }
        return reservationRepository.findByRestaurantIdAndStatusInOrderByReservationTimeAsc(restaurantId, statuses);
    }

    @Transactional
    public Reservation updateStatus(UUID reservationId, UUID restaurantId, ReservationStatus targetStatus) {
        Reservation reservation = getForRestaurant(reservationId, restaurantId);
        if (!reservation.getStatus().canTransitionTo(targetStatus)) {
            throw new BadRequestException(
                    "Cannot move reservation from " + reservation.getStatus() + " to " + targetStatus);
        }
        reservation.setStatus(targetStatus);
        return reservation;
    }

    @Transactional
    public Reservation assignTable(UUID reservationId, UUID restaurantId, UUID tableId) {
        Reservation reservation = getForRestaurant(reservationId, restaurantId);
        RestaurantTable table = tableService.getForRestaurant(tableId, restaurantId);
        requireNoConflict(table.getId(), reservation.getReservationTime(), reservation.getId());
        reservation.setTable(table);
        return reservation;
    }

    /** Rejects a table+time combination that falls within an existing reservation's holding window for that table. */
    private void requireNoConflict(UUID tableId, Instant reservationTime, UUID excludeReservationId) {
        Instant windowStart = reservationTime.minus(RESERVATION_WINDOW);
        Instant windowEnd = reservationTime.plus(RESERVATION_WINDOW);
        boolean conflict = reservationRepository.findByTableIdAndStatusIn(tableId, TABLE_HOLDING_STATUSES).stream()
                .filter(r -> !r.getId().equals(excludeReservationId))
                .anyMatch(r -> r.getReservationTime().isAfter(windowStart) && r.getReservationTime().isBefore(windowEnd));
        if (conflict) {
            throw new ConflictException("This table already has a reservation within 90 minutes of that time");
        }
    }

    private Reservation getForRestaurant(UUID reservationId, UUID restaurantId) {
        return reservationRepository.findByIdAndRestaurantId(reservationId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));
    }
}
