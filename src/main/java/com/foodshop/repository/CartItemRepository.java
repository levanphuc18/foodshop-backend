package com.foodshop.repository;

import com.foodshop.entity.CartItem;
import com.foodshop.entity.CartItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, CartItemId> {
    List<CartItem> findByUser_UserId(Integer userId);
    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.user.userId = :userId AND c.product.productId IN :productIds")
    int deleteSelectedItems(@Param("userId") Integer userId, @Param("productIds") List<Integer> productIds);
}