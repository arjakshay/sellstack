package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") UUID id);

    @Query("SELECT o FROM Order o WHERE o.seller.id = :sellerId AND o.deliveredAt IS NOT NULL " +
            "AND o.deliveredAt BETWEEN :startDate AND :endDate ORDER BY o.deliveredAt DESC")
    List<Order> findDeliveredOrdersBySellerAndDateRange(
            @Param("sellerId") UUID sellerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM Order o WHERE o.seller.id = :sellerId AND o.paymentStatus = 'PAID' " +
            "AND o.deliveredAt IS NULL ORDER BY o.createdAt ASC")
    List<Order> findPendingDeliveryOrders(@Param("sellerId") UUID sellerId);

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM Order o WHERE o.customerEmail = :email ORDER BY o.createdAt DESC")
    List<Order> findByCustomerEmail(@Param("email") String email);
}