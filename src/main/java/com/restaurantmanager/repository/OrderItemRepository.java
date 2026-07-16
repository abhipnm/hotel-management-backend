package com.restaurantmanager.repository;

import com.restaurantmanager.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    @Query("""
            select oi.itemNameSnapshot as name, sum(oi.quantity) as quantitySold, sum(oi.subtotal) as revenue
            from OrderItem oi
            where oi.order.restaurant.id = :restaurantId
              and oi.order.status <> com.restaurantmanager.entity.OrderStatus.CANCELLED
              and oi.order.createdAt >= :from and oi.order.createdAt < :to
            group by oi.itemNameSnapshot
            order by sum(oi.quantity) desc
            """)
    List<TopItemProjection> findTopSellingItems(
            @Param("restaurantId") UUID restaurantId, @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query("""
            select oi.itemNameSnapshot as name, sum(oi.quantity) as quantitySold, sum(oi.subtotal) as revenue
            from OrderItem oi
            where oi.order.restaurant.id = :restaurantId
              and oi.order.status <> com.restaurantmanager.entity.OrderStatus.CANCELLED
              and oi.order.createdAt >= :from and oi.order.createdAt < :to
            group by oi.itemNameSnapshot
            order by sum(oi.quantity) asc
            """)
    List<TopItemProjection> findLeastSellingItems(
            @Param("restaurantId") UUID restaurantId, @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    interface TopItemProjection {
        String getName();

        Long getQuantitySold();

        BigDecimal getRevenue();
    }
}
