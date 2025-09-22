package com.foodshop.mapper;

import com.foodshop.dto.request.DiscountRequest;
import com.foodshop.dto.response.DiscountResponse;
import com.foodshop.entity.Discount;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DiscountMapper {
    Discount toDiscount(DiscountRequest request);
    DiscountResponse toDiscountResponse(Discount discount);
    void updateDiscountFromRequest(DiscountRequest request, @MappingTarget Discount discount);
}
