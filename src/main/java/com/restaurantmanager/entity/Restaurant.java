package com.restaurantmanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    /** An externally-hosted logo URL, used only when no logo has been uploaded (see logoImage below). */
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /** Bytes of an uploaded logo image; takes precedence over logoUrl when present. */
    @Column(name = "logo_image", columnDefinition = "bytea")
    private byte[] logoImage;

    /** MIME type of logoImage (e.g. "image/png"); null when no image has been uploaded. */
    @Column(name = "logo_image_content_type", length = 100)
    private String logoImageContentType;

    /** Brand color preset applied across the dashboard and guest app. Vegetarian-only restaurants always show green regardless of this setting. */
    @Enumerated(EnumType.STRING)
    @Column(name = "theme_color", nullable = false, length = 20)
    @Builder.Default
    private ThemeColor themeColor = ThemeColor.ORANGE;

    /** Short line shown under the restaurant name on the guest ordering page. */
    @Column(length = 150)
    private String tagline;
}
