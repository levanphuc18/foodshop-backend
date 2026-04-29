package com.foodshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrderRequest {

    @NotBlank(message = "Shipping address is required")
    @Size(max = 255, message = "Shipping address must not exceed 255 characters")
    private String shippingAddress;

    @Size(max = 255, message = "Shipping note must not exceed 255 characters")
    private String shippingNote;

    /**
     * Mã coupon (ORDER discount).
     * Nếu null → không áp dụng mã giảm giá.
     */
    private String discountCode;
    private List<String> discountCodes = new ArrayList<>();
}
