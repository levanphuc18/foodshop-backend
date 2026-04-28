package com.foodshop.dto.email;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class OrderEmailModel {
    Integer orderId;
    String customerName;
    String customerEmail;
    String status;
    String statusLabel;
    String statusMessage;
    String createdAtFormatted;
    String shippingAddress;
    String shippingNote;
    String discountCode;
    String orderUrl;
    BigDecimal totalAmount;
    BigDecimal discountAmount;
    BigDecimal shippingFee;
    BigDecimal shippingDiscount;
    BigDecimal finalAmount;
    List<OrderEmailItemModel> items;
}
