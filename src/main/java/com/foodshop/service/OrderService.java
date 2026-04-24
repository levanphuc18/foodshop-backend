package com.foodshop.service;

import com.foodshop.dto.request.OrderRequest;
import com.foodshop.dto.response.OrderResponse;
import com.foodshop.enums.OrderStatus;

import java.util.List;

public interface OrderService {
    /** Tạo đơn hàng từ toàn bộ giỏ hàng của user. */
    OrderResponse createOrder(Integer userId, OrderRequest request);

    /** Lấy tất cả đơn hàng của một user (dành cho User). */
    List<OrderResponse> getOrdersByUser(Integer userId);

    /** Lấy tất cả đơn hàng (dành cho Admin). */
    List<OrderResponse> getAllOrders();

    /** Lấy chi tiết một đơn hàng. */
    OrderResponse getOrderById(Integer orderId);

    /** Admin cập nhật trạng thái đơn hàng. */
    OrderResponse updateOrderStatus(Integer orderId, OrderStatus status);
}
