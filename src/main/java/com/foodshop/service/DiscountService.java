package com.foodshop.service;

import com.foodshop.dto.request.DiscountRequest;
import com.foodshop.dto.response.CouponValidationResponse;
import com.foodshop.dto.response.DiscountResponse;
import com.foodshop.entity.Discount;
import com.foodshop.enums.DiscountStatus;
import com.foodshop.enums.DiscountType;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface DiscountService {
    DiscountResponse createDiscount(DiscountRequest request);

    DiscountResponse updateDiscount(Integer id, DiscountRequest request);

    void deleteDiscount(Integer id);

    DiscountResponse getDiscountById(Integer id);

    List<DiscountResponse> getAllDiscounts();

    Page<DiscountResponse> getAllDiscountsAdmin(String keyword, DiscountStatus status, DiscountType type, int page, int size, String sortBy, boolean asc);

    DiscountResponse toggleDiscountStatus(Integer id);

    CouponValidationResponse validateCoupon(String code, BigDecimal totalAmount, Integer userId);

    Discount validateDiscountForCheckout(String code, BigDecimal totalAmount, Integer userId);

    BigDecimal calculateDiscountAmount(Discount discount, BigDecimal baseAmount);

    void reserveUsage(Discount discount);

    void releaseUsage(Discount discount);
}
