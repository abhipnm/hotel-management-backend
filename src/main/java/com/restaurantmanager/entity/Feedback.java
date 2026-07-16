package com.restaurantmanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A guest's overall rating for a visit. Scoped to one guest session (a re-submit
 * updates the existing row). Guest name and table number are snapshotted so the
 * feedback stays readable after the session expires or is closed — mirroring the
 * snapshot approach used by {@link OrderItem}.
 */
@Getter
@Setter
@Entity
@Table(name = "feedback")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Feedback extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_session_id", nullable = false)
    private GuestSession guestSession;

    @Column(nullable = false)
    private int rating;

    @Column(length = 1000)
    private String comment;

    @Column(name = "guest_name_snapshot", nullable = false)
    private String guestNameSnapshot;

    @Column(name = "table_number_snapshot", nullable = false)
    private String tableNumberSnapshot;
}
