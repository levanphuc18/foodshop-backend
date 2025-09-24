package com.foodshop.dto.response;

import lombok.*;

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
    private Integer quantity;
}