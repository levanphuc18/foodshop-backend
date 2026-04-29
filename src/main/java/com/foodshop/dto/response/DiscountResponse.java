package com.foodshop.dto.response;

import com.foodshop.enums.DiscountStatus;
import com.foodshop.enums.DiscountType;
import com.foodshop.enums.DiscountUnit;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DiscountResponse {
    private Integer discountId;
    private String code;
    private DiscountType type;
    private DiscountUnit discountUnit;
    private BigDecimal value;
    private BigDecimal maxDiscount;
    private BigDecimal minOrderAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private DiscountStatus status;
    private Integer usageLimit;
    private Integer usedCount;
    private Integer perUserLimit;
}
