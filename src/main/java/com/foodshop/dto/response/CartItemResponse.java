package com.foodshop.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {
    private Integer userId;
    private String username;
    private Integer productId;
    private String productName;
    private BigDecimal productPrice;
    private String productImageUrl;
    private Integer quantity;
}