package com.foodshop.dto.email;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class OrderEmailItemModel {
    String productName;
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal originalUnitPrice;
    BigDecimal subtotal;
    String imageUrl;
}
