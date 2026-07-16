package com.restaurantmanager.repository;

import com.restaurantmanager.entity.Order;
import com.restaurantmanager.entity.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @EntityGraph(attributePaths = {"table", "guestSession", "items", "items.menuItem"})
    Optional<Order> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    @EntityGraph(attributePaths = {"table", "guestSession", "items", "items.menuItem"})
    Optional<Order> findByIdAndGuestSessionId(UUID id, UUID guestSessionId);

    @EntityGraph(attributePaths = {"table", "guestSession", "items", "items.menuItem"})
    List<Order> findByGuestSessionIdOrderByCreatedAtDesc(UUID guestSessionId);

    @EntityGraph(attributePaths = {"table", "guestSession", "items", "items.menuItem"})
    List<Order> findByRestaurantIdOrderByCreatedAtDesc(UUID restaurantId);

    @EntityGraph(attributePaths = {"table", "guestSession", "items", "items.menuItem"})
    List<Order> findByRestaurantIdAndStatusInOrderByCreatedAtAsc(UUID restaurantId, Collection<OrderStatus> statuses);

    @Query("""
            select coalesce(sum(o.totalAmount), 0) from Order o
            where o.restaurant.id = :restaurantId
              and o.status <> com.restaurantmanager.entity.OrderStatus.CANCELLED
              and o.createdAt >= :from and o.createdAt < :to
            """)
    BigDecimal sumRevenue(@Param("restaurantId") UUID restaurantId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select count(o) from Order o
            where o.restaurant.id = :restaurantId
              and o.status <> com.restaurantmanager.entity.OrderStatus.CANCELLED
              and o.createdAt >= :from and o.createdAt < :to
            """)
    long countOrders(@Param("restaurantId") UUID restaurantId, @Param("from") Instant from, @Param("to") Instant to);

    /**
     * Average time from placement to being marked served, in seconds, for orders
     * actually served within the window. Null when none were served in range
     * (avg() over zero rows) — callers must handle that. Native query: Postgres's
     * EXTRACT(EPOCH FROM ...) on an interval is unambiguous, unlike HQL date-diff.
     */
    @Query(value = """
            select avg(extract(epoch from (served_at - created_at)))
            from orders
            where restaurant_id = :restaurantId
              and served_at is not null
              and served_at >= :from and served_at < :to
            """, nativeQuery = true)
    Double averageServingSeconds(@Param("restaurantId") UUID restaurantId, @Param("from") Instant from, @Param("to") Instant to);

    /** Per-waiter serving stats for orders they marked SERVED within the window. Native query for the same EXTRACT(EPOCH) reason as {@link #averageServingSeconds}. */
    @Query(value = """
            select u.name as waiterName,
                   count(*) as ordersServed,
                   coalesce(sum(o.total_amount), 0) as revenue,
                   avg(extract(epoch from (o.served_at - o.created_at))) as avgServingSeconds
            from orders o
            join app_users u on u.id = o.served_by_user_id
            where o.restaurant_id = :restaurantId
              and o.served_at is not null
              and o.served_at >= :from and o.served_at < :to
            group by u.id, u.name
            order by count(*) desc
            """, nativeQuery = true)
    List<WaiterPerformanceProjection> findWaiterPerformance(
            @Param("restaurantId") UUID restaurantId, @Param("from") Instant from, @Param("to") Instant to);

    interface WaiterPerformanceProjection {
        String getWaiterName();

        Long getOrdersServed();

        BigDecimal getRevenue();

        Double getAvgServingSeconds();
    }
}
