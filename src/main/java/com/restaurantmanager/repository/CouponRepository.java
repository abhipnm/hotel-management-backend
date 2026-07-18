package com.restaurantmanager.repository;

import com.restaurantmanager.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    List<Coupon> findByRestaurantIdOrderByCreatedAtDesc(UUID restaurantId);

    Optional<Coupon> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    Optional<Coupon> findByRestaurantIdAndCodeIgnoreCase(UUID restaurantId, String code);

    boolean existsByRestaurantIdAndCodeIgnoreCase(UUID restaurantId, String code);
}
