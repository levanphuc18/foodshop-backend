package com.foodshop.dto.response;

import com.foodshop.enums.DiscountType;
import com.foodshop.enums.DiscountUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationResponse {
    private boolean valid;
    private String code;
    private DiscountType type;
    private DiscountUnit discountUnit;
    private BigDecimal value;
    /** Số tiền thực tế được giảm (đã tính theo đơn hàng cụ thể) */
    private BigDecimal discountAmount;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscount;
    /** Thông báo cho người dùng */
    private String message;
}
