package com.foodshop.controller;

import com.foodshop.dto.ApiResponse;
import com.foodshop.dto.response.CouponValidationResponse;
import com.foodshop.dto.response.DiscountResponse;
import com.foodshop.exception.GlobalCode;
import com.foodshop.service.AuthService;
import com.foodshop.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/discounts")
public class DiscountController {

    private final DiscountService discountService;
    private final AuthService authService;

    /**
     * Public: Lấy danh sách discount ACTIVE (cho phép user xem các voucher đang chạy).
     * GET /api/v1/discounts
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DiscountResponse>>> getAllActiveDiscounts() {
        List<DiscountResponse> responses = discountService.getAllDiscounts();
        return ResponseEntity.ok(new ApiResponse<>(GlobalCode.SUCCESS, "Discounts retrieved successfully.", responses));
    }

    /**
     * Authenticated user: Validate coupon code và preview số tiền được giảm.
     * POST /api/v1/discounts/validate?totalAmount=500000
     */
    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> validateCoupon(
            @RequestParam String code,
            @RequestParam BigDecimal totalAmount,
            Authentication authentication) {

        Integer userId = authentication == null ? null : authService.getUserIdByUsername(authentication.getName());
        CouponValidationResponse result = discountService.validateCoupon(code, totalAmount, userId);
        return ResponseEntity.ok(new ApiResponse<>(GlobalCode.SUCCESS, result.getMessage(), result));
    }
}
