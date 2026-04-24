package com.foodshop.controller;

import com.foodshop.dto.ApiResponse;
import com.foodshop.dto.response.OrderResponse;
import com.foodshop.enums.OrderStatus;
import com.foodshop.exception.GlobalCode;
import com.foodshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * Admin lấy tất cả đơn hàng.
     * GET /api/v1/admin/orders
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        List<OrderResponse> responses = orderService.getAllOrders();
        return ResponseEntity.ok(
                new ApiResponse<>(GlobalCode.SUCCESS, "All orders fetched successfully.", responses));
    }

    /**
     * Admin cập nhật trạng thái đơn hàng.
     * PATCH /api/v1/admin/orders/{id}/status?status=CONFIRMED
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Integer id,
            @RequestParam OrderStatus status) {

        OrderResponse response = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(
                new ApiResponse<>(GlobalCode.SUCCESS, "Order status updated.", response));
    }
}