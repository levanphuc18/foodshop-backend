package com.foodshop.service;

import com.foodshop.dto.response.CouponValidationResponse;
import com.foodshop.dto.response.DiscountResponse;
import com.foodshop.dto.request.DiscountRequest;
import java.math.BigDecimal;
import java.util.List;

public interface DiscountService {
    DiscountResponse createDiscount(DiscountRequest request);
    DiscountResponse updateDiscount(Integer id, DiscountRequest request);
    void deleteDiscount(Integer id);
    DiscountResponse getDiscountById(Integer id);
    List<DiscountResponse> getAllDiscounts();
    DiscountResponse toggleDiscountStatus(Integer id);
    /** Validate coupon code và tính toán số tiền được giảm (preview trước khi đặt hàng) */
    CouponValidationResponse validateCoupon(String code, BigDecimal totalAmount);
}

