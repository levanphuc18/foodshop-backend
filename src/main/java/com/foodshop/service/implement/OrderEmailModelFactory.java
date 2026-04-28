package com.foodshop.service.implement;

import com.foodshop.dto.email.OrderEmailItemModel;
import com.foodshop.dto.email.OrderEmailModel;
import com.foodshop.entity.Order;
import com.foodshop.entity.OrderItem;
import com.foodshop.entity.Product;
import com.foodshop.enums.OrderStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class OrderEmailModelFactory {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public OrderEmailModel buildForCreated(Order order) {
        return baseBuilder(order)
                .statusLabel(labelForStatus(order.getStatus()))
                .statusMessage("We have received your order and will process it shortly.")
                .build();
    }

    public OrderEmailModel buildForStatusUpdate(Order order) {
        return baseBuilder(order)
                .statusLabel(labelForStatus(order.getStatus()))
                .statusMessage(messageForStatus(order.getStatus()))
                .build();
    }

    private OrderEmailModel.OrderEmailModelBuilder baseBuilder(Order order) {
        return OrderEmailModel.builder()
                .orderId(order.getOrderId())
                .customerName(order.getUser() != null ? order.getUser().getFullName() : "Customer")
                .customerEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .status(order.getStatus().name())
                .createdAtFormatted(order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_TIME_FORMATTER) : "")
                .shippingAddress(order.getShippingAddress())
                .shippingNote(order.getShippingNote())
                .discountCode(order.getDiscountCode())
                .orderUrl(frontendUrl + "/orders/" + order.getOrderId())
                .totalAmount(safeAmount(order.getTotalAmount()))
                .discountAmount(safeAmount(order.getDiscountAmount()))
                .shippingFee(safeAmount(order.getShippingFee()))
                .shippingDiscount(safeAmount(order.getShippingDiscount()))
                .finalAmount(safeAmount(order.getFinalAmount()))
                .items(mapItems(order.getOrderItems()));
    }

    private List<OrderEmailItemModel> mapItems(List<OrderItem> orderItems) {
        if (orderItems == null) {
            return List.of();
        }

        return orderItems.stream()
                .map(this::mapItem)
                .toList();
    }

    private OrderEmailItemModel mapItem(OrderItem item) {
        Product product = item.getProduct();
        String imageUrl = null;
        if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
            imageUrl = product.getImages().get(0).getImageUrl();
        }

        return OrderEmailItemModel.builder()
                .productName(product != null ? product.getName() : "Product")
                .quantity(item.getQuantity())
                .unitPrice(safeAmount(item.getPrice()))
                .subtotal(safeAmount(item.getSubtotal()))
                .imageUrl(imageUrl)
                .build();
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String labelForStatus(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Pending";
            case PAID -> "Paid";
            case CONFIRMED -> "Confirmed";
            case SHIPPED -> "Shipped";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
        };
    }

    private String messageForStatus(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Your order has been created and is waiting for confirmation.";
            case PAID -> "We have recorded your payment successfully.";
            case CONFIRMED -> "Your order has been confirmed and is being prepared.";
            case SHIPPED -> "Your order is on the way.";
            case COMPLETED -> "Your order has been completed. Thank you for shopping with Foodshop.";
            case CANCELLED -> "Your order has been cancelled. Please contact support if this was unexpected.";
        };
    }
}
