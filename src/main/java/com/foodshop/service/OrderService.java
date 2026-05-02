package com.foodshop.service;

import com.foodshop.dto.request.OrderRequest;
import com.foodshop.dto.response.OrderResponse;
import com.foodshop.enums.OrderStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(Integer userId, OrderRequest request);

    List<OrderResponse> getOrdersByUser(Integer userId);

    Page<OrderResponse> getAllOrdersAdmin(String keyword, OrderStatus status, int page, int size, String sortBy, boolean asc);

    OrderResponse getOrderById(Integer orderId, Integer callerUserId);

    OrderResponse updateOrderStatus(Integer orderId, OrderStatus status);
}
