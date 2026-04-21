package com.foodshop.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ProductResponse {
    private Integer productId;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal salePrice;
    private BigDecimal discountPercentage;
    private Integer quantity;
    private List<String> imageUrls;
    private Integer discountId;
    private Integer categoryId;
}