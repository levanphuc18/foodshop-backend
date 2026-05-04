package com.foodshop.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {

    @NotNull(message = "Product ID is required")
    private Integer productId;

    @NotNull(message = "Order ID is required")
    private Integer orderId;

    @NotNull(message = "Order Item ID is required")
    private Integer orderItemId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must not exceed 5")
    private Integer rating;

    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String comment;

    private List<String> imageUrls;
    private List<org.springframework.web.multipart.MultipartFile> imageFiles;
}
