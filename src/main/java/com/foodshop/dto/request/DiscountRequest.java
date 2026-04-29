package com.foodshop.dto.request;

import com.foodshop.enums.DiscountStatus;
import com.foodshop.enums.DiscountType;
import com.foodshop.enums.DiscountUnit;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DiscountRequest {
    @NotBlank(message = "Discount code is required")
    private String code;

    @NotNull(message = "Discount type is required")
    private DiscountType type;

    @NotNull(message = "Discount unit is required")
    private DiscountUnit discountUnit;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Discount value must be greater than 0")
    private BigDecimal value;

    private BigDecimal maxDiscount;

    private BigDecimal minOrderAmount;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Status is required")
    private DiscountStatus status;

    @Positive(message = "Usage limit must be greater than 0 when provided")
    private Integer usageLimit;

    @Positive(message = "Per-user limit must be greater than 0 when provided")
    private Integer perUserLimit;

}
