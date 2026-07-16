package com.restaurantmanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "restaurants")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Restaurant extends BaseEntity {

    @Column(nullable = false)
    private String name;

    /** URL-friendly unique identifier, e.g. used in public menu links. */
    @Column(nullable = false, unique = true)
    private String slug;

    @Column
    private String address;

    @Column
    private String phone;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** When true, the restaurant serves only vegetarian food and every menu item must be VEG. */
    @Column(name = "veg_only", nullable = false)
    @Builder.Default
    private boolean vegOnly = false;
}
