package com.foodshop.repository;

import com.foodshop.entity.OrderAppliedDiscount;
import com.foodshop.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderAppliedDiscountRepository extends JpaRepository<OrderAppliedDiscount, Integer> {
    @Query("""
            SELECT COUNT(oad)
            FROM OrderAppliedDiscount oad
            WHERE oad.discount.discountId = :discountId
              AND oad.order.user.userId = :userId
              AND oad.order.status <> :excludedStatus
            """)
    long countActiveUsageByDiscountAndUser(
            @Param("discountId") Integer discountId,
            @Param("userId") Integer userId,
            @Param("excludedStatus") OrderStatus excludedStatus
    );
}
