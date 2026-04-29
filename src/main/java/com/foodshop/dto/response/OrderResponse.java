package com.foodshop.dto.response;

import com.foodshop.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Integer orderId;
    private String shippingAddress;
    private String shippingNote;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal shippingDiscount;
    private BigDecimal finalAmount;
    private String discountCode;
    private List<String> discountCodes;
    private Integer userId;
    private String username;
    private String fullName;
    private List<OrderItemResponse> orderItems;
}
