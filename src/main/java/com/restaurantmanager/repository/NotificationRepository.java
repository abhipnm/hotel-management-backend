package com.restaurantmanager.repository;

import com.restaurantmanager.entity.Notification;
import com.restaurantmanager.entity.NotificationAudience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRestaurantIdAndAudienceOrderByCreatedAtDesc(UUID restaurantId, NotificationAudience audience);

    long countByRestaurantIdAndAudienceAndReadFalse(UUID restaurantId, NotificationAudience audience);

    @Modifying
    @Query("update Notification n set n.read = true " +
            "where n.restaurant.id = :restaurantId and n.audience = com.restaurantmanager.entity.NotificationAudience.STAFF and n.read = false")
    void markAllStaffRead(@Param("restaurantId") UUID restaurantId);

    @Query("select n from Notification n where n.restaurant.id = :restaurantId " +
            "and n.audience = com.restaurantmanager.entity.NotificationAudience.GUEST " +
            "and (n.guestSession.id = :guestSessionId or n.guestSession is null) " +
            "order by n.createdAt desc")
    List<Notification> findForGuestSession(@Param("restaurantId") UUID restaurantId, @Param("guestSessionId") UUID guestSessionId);

    @Query("select count(n) from Notification n where n.restaurant.id = :restaurantId " +
            "and n.audience = com.restaurantmanager.entity.NotificationAudience.GUEST " +
            "and (n.guestSession.id = :guestSessionId or n.guestSession is null) and n.read = false")
    long countUnreadForGuestSession(@Param("restaurantId") UUID restaurantId, @Param("guestSessionId") UUID guestSessionId);

    @Modifying
    @Query("update Notification n set n.read = true where n.restaurant.id = :restaurantId " +
            "and n.audience = com.restaurantmanager.entity.NotificationAudience.GUEST " +
            "and (n.guestSession.id = :guestSessionId or n.guestSession is null) and n.read = false")
    void markAllGuestRead(@Param("restaurantId") UUID restaurantId, @Param("guestSessionId") UUID guestSessionId);
}
