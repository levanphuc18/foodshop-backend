package com.foodshop.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemRequest {
    @NotNull(message = "UserId is required")
    private Integer userId;

    @NotNull(message = "ProductId is required")
    private Integer productId;

    @NotNull(message = "Quantity is required")
    @Min(1)
    private Integer quantity;
}