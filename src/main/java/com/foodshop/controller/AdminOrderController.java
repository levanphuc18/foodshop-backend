package com.foodshop.controller;

import com.foodshop.dto.ApiResponse;
import com.foodshop.dto.response.OrderResponse;
import com.foodshop.dto.response.PageResponse;
import com.foodshop.enums.OrderStatus;
import com.foodshop.exception.GlobalCode;
import com.foodshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(defaultValue = "false") boolean asc) {
        PageResponse<OrderResponse> responses = PageResponse.from(
                orderService.getAllOrdersAdmin(
                        search != null ? search : keyword,
                        status,
                        page,
                        size,
                        sortBy,
                        normalizeAsc(sortDir, asc)
                )
        );
        return ResponseEntity.ok(
                new ApiResponse<>(GlobalCode.SUCCESS, "All orders fetched successfully.", responses));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Integer id,
            @RequestParam OrderStatus status) {

        OrderResponse response = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(
                new ApiResponse<>(GlobalCode.SUCCESS, "Order status updated.", response));
    }

    private boolean normalizeAsc(String sortDir, boolean asc) {
        if (sortDir == null || sortDir.isBlank()) {
            return asc;
        }
        return "ASC".equalsIgnoreCase(sortDir.trim().toUpperCase(Locale.ROOT));
    }
}
