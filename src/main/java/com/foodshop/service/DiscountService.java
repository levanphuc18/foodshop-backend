package com.foodshop.service;

import com.foodshop.dto.response.DiscountResponse;
import com.foodshop.dto.request.DiscountRequest;
import java.util.List;

public interface DiscountService {
    DiscountResponse createDiscount(DiscountRequest request);
    DiscountResponse updateDiscount(Integer id, DiscountRequest request);
    void deleteDiscount(Integer id);
    DiscountResponse getDiscountById(Integer id);
    List<DiscountResponse> getAllDiscounts();
}
