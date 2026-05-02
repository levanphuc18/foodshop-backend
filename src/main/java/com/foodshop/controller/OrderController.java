package com.foodshop.controller;

import com.foodshop.dto.ApiResponse;
import com.foodshop.dto.request.OrderRequest;
import com.foodshop.dto.response.OrderResponse;
import com.foodshop.security.CustomUserDetails;
import com.foodshop.exception.GlobalCode;
import com.foodshop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * User tạo đơn hàng từ giỏ hàng hiện tại.
     * POST /api/v1/orders
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OrderRequest request) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(GlobalCode.UNAUTHORIZED, "Please log in to create an order.", null));
        }

        OrderResponse response = orderService.createOrder(userDetails.getUser().getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(GlobalCode.SUCCESS, "Order created successfully.", response));
    }

    /**
     * User lấy danh sách đơn hàng của mình.
     * GET /api/v1/orders/my
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(GlobalCode.UNAUTHORIZED, "Please log in to view your orders.", null));
        }

        List<OrderResponse> responses = orderService.getOrdersByUser(userDetails.getUser().getUserId());
        return ResponseEntity.ok(
                new ApiResponse<>(GlobalCode.SUCCESS, "Orders fetched successfully.", responses));
    }

    /**
     * Lấy chi tiết một đơn hàng của mình.
     * SECURITY FIX [P0-IDOR]: Truyền userId của user đang đăng nhập để service kiểm tra owner.
     * GET /api/v1/orders/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(GlobalCode.UNAUTHORIZED, "Please log in.", null));
        }

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Integer callerUserId = isAdmin ? null : userDetails.getUser().getUserId();

        OrderResponse response = orderService.getOrderById(id, callerUserId);
        return ResponseEntity.ok(
                new ApiResponse<>(GlobalCode.SUCCESS, "Order fetched successfully.", response));
    }
}
