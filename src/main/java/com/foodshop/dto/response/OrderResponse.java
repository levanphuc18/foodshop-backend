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

    /** Tổng tiền hàng (chưa trừ coupon ORDER) */
    private BigDecimal totalAmount;

    /** Số tiền được giảm bởi ORDER coupon */
    private BigDecimal discountAmount;

    /** Phí giao hàng cơ bản */
    private BigDecimal shippingFee;

    /** Số tiền phí giao hàng được giảm bởi SHIPPING coupon */
    private BigDecimal shippingDiscount;

    /** Thành tiền cuối cùng = totalAmount + shippingFee - discountAmount - shippingDiscount */
    private BigDecimal finalAmount;

    /** Mã coupon đã áp dụng (nếu có) */
    private String discountCode;

    private Integer userId;
    private String username;
    private String fullName;
    private List<OrderItemResponse> orderItems;
}
