package com.foodshop.dto.request;

import com.foodshop.enums.DiscountStatus;
import com.foodshop.enums.DiscountType;
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

    @NotNull(message = "Discount value is required")
    @DecimalMax(value = "100.0", message = "Discount cannot exceed 100%")
    private BigDecimal value;

    private BigDecimal minOrderAmount;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Status is required")
    private DiscountStatus status;
}