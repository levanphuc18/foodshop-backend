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
    /** Loại discount đang áp dụng ("PRODUCT" hoặc null nếu không có) */
    private String discountType;
    /** Đơn vị discount đang áp dụng ("PERCENT" hoặc "AMOUNT", null nếu không có) */
    private String discountUnit;
    private Integer quantity;
    private List<String> imageUrls;
    private Integer discountId;
    private Integer categoryId;
    private BigDecimal maxDiscount;
    private String productStatus;
    private Boolean isActive;
    private BigDecimal averageRating;
    private Integer totalReviews;
}
