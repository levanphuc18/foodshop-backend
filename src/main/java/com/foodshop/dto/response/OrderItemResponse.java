package com.foodshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private Integer orderItemId;
    private Integer productId;
    private String productName;
    private String productImageUrl;

    /** Giá đã snapshot tại thời điểm mua (salePrice nếu có, ngược lại là price gốc) */
    private BigDecimal price;

    private Integer quantity;

    /** Thành tiền = price * quantity */
    private BigDecimal subtotal;
}
