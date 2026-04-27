package com.foodshop.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkAssignDiscountRequest {
    
    @NotEmpty(message = "Product IDs list cannot be empty")
    private List<Integer> productIds;

    // Optional: if null, it will clear the discount from the products
    private Integer discountId;
}
