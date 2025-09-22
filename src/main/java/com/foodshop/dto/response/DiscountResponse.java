package com.foodshop.dto.response;

import com.foodshop.enums.DiscountStatus;
import com.foodshop.enums.DiscountType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DiscountResponse {
    private Integer discountId;
    private String code;
    private DiscountType type;
    private BigDecimal value;
    private BigDecimal minOrderAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private DiscountStatus status;
}
