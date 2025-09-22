package com.foodshop.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {
    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @NotNull(message = "Quantity is required")
    @Min(0)
    private Integer quantity;

    private MultipartFile imageFile;

    private Integer discountId;

    @NotNull(message = "Category id is required")
    private Integer categoryId;
}