package com.foodshop.service;

import com.foodshop.dto.request.CartItemRequest;
import com.foodshop.dto.response.CartItemResponse;
import java.util.List;

public interface CartItemService {
    CartItemResponse addToCart(CartItemRequest request);
    List<CartItemResponse> getCartItemsByUser(Integer userId);
    CartItemResponse updateQuantity(Integer userId, Integer productId, Integer quantity);
    void removeCartItem(Integer userId, Integer productId);
    void clearCart(Integer userId);
    void removeSelectedItems(Integer userId, List<Integer> productIds);
}