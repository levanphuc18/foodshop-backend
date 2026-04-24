package com.foodshop.service.implement;

import com.foodshop.dto.request.OrderRequest;
import com.foodshop.dto.response.OrderItemResponse;
import com.foodshop.dto.response.OrderResponse;
import com.foodshop.entity.*;
import com.foodshop.enums.DiscountStatus;
import com.foodshop.enums.DiscountType;
import com.foodshop.enums.OrderStatus;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.repository.*;
import com.foodshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final DiscountRepository discountRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(Integer userId, OrderRequest request) {
        // 1. Xác minh user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(GlobalCode.USER_NOT_FOUND));

        // 2. Lấy giỏ hàng
        List<CartItem> cartItems = cartItemRepository.findByUser_UserId(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new GlobalException(GlobalCode.CART_NOT_ITEM);
        }

        // 3. Kiểm tra tồn kho và tạo OrderItems với snapshot giá
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            // Kiểm tra tồn kho
            if (product.getQuantity() < cartItem.getQuantity()) {
                throw new GlobalException(GlobalCode.INSUFFICIENT_STOCK);
            }

            // Snapshot giá: ưu tiên salePrice (nếu discount PRODUCT còn hiệu lực), fallback price gốc
            BigDecimal snapshotPrice = resolveSalePrice(product);

            BigDecimal subtotal = snapshotPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(snapshotPrice)
                    .subtotal(subtotal)
                    .build();
            orderItems.add(orderItem);

            totalAmount = totalAmount.add(subtotal);

            // Trừ tồn kho
            product.setQuantity(product.getQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        // 4. Xử lý ORDER / SHIPPING discount (coupon nhập tay)
        BigDecimal baseShippingFee = new BigDecimal("30000"); // Mặc định phí ship là 30k
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal shippingDiscount = BigDecimal.ZERO;
        Discount appliedDiscount = null;
        String appliedCode = null;

        if (request.getDiscountCode() != null && !request.getDiscountCode().isBlank()) {
            appliedDiscount = discountRepository.findByCode(request.getDiscountCode().trim())
                    .orElseThrow(() -> new GlobalException(GlobalCode.DISCOUNT_NOT_FOUND));

            // Validate: phải là type ORDER hoặc SHIPPING
            if (appliedDiscount.getType() != DiscountType.ORDER
                    && appliedDiscount.getType() != DiscountType.SHIPPING) {
                throw new GlobalException(GlobalCode.DISCOUNT_NOT_VALID);
            }

            // Validate: phải ACTIVE và trong thời hạn
            LocalDate today = LocalDate.now();
            if (appliedDiscount.getStatus() != com.foodshop.enums.DiscountStatus.ACTIVE
                    || today.isBefore(appliedDiscount.getStartDate())
                    || today.isAfter(appliedDiscount.getEndDate())) {
                throw new GlobalException(GlobalCode.DISCOUNT_NOT_VALID);
            }

            // Validate: đơn hàng đủ giá trị tối thiểu
            if (appliedDiscount.getMinOrderAmount() != null
                    && totalAmount.compareTo(appliedDiscount.getMinOrderAmount()) < 0) {
                throw new GlobalException(GlobalCode.DISCOUNT_NOT_APPLICABLE);
            }

            if (appliedDiscount.getType() == DiscountType.ORDER) {
                if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new GlobalException(GlobalCode.DISCOUNT_NOT_APPLICABLE, "Đơn hàng đã là 0đ, không thể áp dụng thêm mã giảm giá đơn hàng.");
                }
                // Tính giảm giá đơn hàng theo unit (PERCENT hoặc AMOUNT)
                discountAmount = calculateDiscountAmount(appliedDiscount, totalAmount);
            } else {
                // SHIPPING: giảm phí vận chuyển, không được vượt quá phí ship cơ bản
                shippingDiscount = appliedDiscount.getValue().min(baseShippingFee);
            }

            appliedCode = appliedDiscount.getCode();
        }

        // 5. Tính finalAmount = (Tiền hàng - giảm giá hàng) + (Phí ship - giảm phí ship)
        BigDecimal finalTotalAmount = totalAmount.subtract(discountAmount);
        if (finalTotalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalTotalAmount = BigDecimal.ZERO;
        }
        BigDecimal finalShippingFee = baseShippingFee.subtract(shippingDiscount);
        if (finalShippingFee.compareTo(BigDecimal.ZERO) < 0) {
            finalShippingFee = BigDecimal.ZERO;
        }
        BigDecimal finalAmount = finalTotalAmount.add(finalShippingFee).setScale(2, RoundingMode.HALF_UP);

        // 6. Tạo Order
        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingNote(request.getShippingNote());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setShippingFee(baseShippingFee);
        order.setShippingDiscount(shippingDiscount);
        order.setFinalAmount(finalAmount);
        order.setDiscount(appliedDiscount);
        order.setDiscountCode(appliedCode);

        // Gắn order vào từng orderItem
        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }
        order.setOrderItems(orderItems);

        Order saved = orderRepository.save(order);

        // 7. Xoá giỏ hàng sau khi đặt hàng thành công
        cartItemRepository.deleteAll(cartItems);

        return toOrderResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Integer userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(GlobalCode.USER_NOT_FOUND));
        return orderRepository.findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new GlobalException(GlobalCode.ORDER_NOT_FOUND));
        return toOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Integer orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new GlobalException(GlobalCode.ORDER_NOT_FOUND));

        OrderStatus currentStatus = order.getStatus();

        // 1. Nếu đơn hàng đã Hoàn thành hoặc đã Hủy thì không được đổi nữa
        if (currentStatus == OrderStatus.COMPLETED || currentStatus == OrderStatus.CANCELLED) {
            throw new GlobalException(GlobalCode.BAD_REQUEST, "Không thể thay đổi trạng thái của đơn hàng đã kết thúc.");
        }

        // 2. Kiểm tra logic chuyển đổi hợp lệ
        boolean isValid = switch (currentStatus) {
            case PENDING -> List.of(OrderStatus.PAID, OrderStatus.CONFIRMED, OrderStatus.CANCELLED).contains(newStatus);
            case PAID -> List.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED).contains(newStatus);
            case CONFIRMED -> List.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED).contains(newStatus);
            case SHIPPED -> List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED).contains(newStatus);
            default -> false;
        };

        if (!isValid && currentStatus != newStatus) {
            throw new GlobalException(GlobalCode.BAD_REQUEST, 
                "Chuyển đổi trạng thái từ " + currentStatus + " sang " + newStatus + " là không hợp lệ.");
        }

        order.setStatus(newStatus);
        return toOrderResponse(orderRepository.save(order));
    }

    // ────────────────────────────────────────────────
    // Private helpers
    // ────────────────────────────────────────────────

    /**
     * Tính giá snapshot cho sản phẩm:
     * - Nếu có PRODUCT discount ACTIVE và trong thời hạn → trả về salePrice
     * - Ngược lại → trả về price gốc
     */
    private BigDecimal resolveSalePrice(Product product) {
        Discount discount = product.getDiscount();
        if (discount != null
                && discount.getType() == DiscountType.PRODUCT
                && discount.getStatus() == DiscountStatus.ACTIVE
                && discount.getValue() != null
                && !LocalDate.now().isBefore(discount.getStartDate())
                && !LocalDate.now().isAfter(discount.getEndDate())) {

            BigDecimal salePrice;
            if (discount.getDiscountUnit() == com.foodshop.enums.DiscountUnit.AMOUNT) {
                // Giảm số tiền cố định
                salePrice = product.getPrice().subtract(discount.getValue())
                        .setScale(2, RoundingMode.HALF_UP);
                // Không để salePrice âm
                if (salePrice.compareTo(BigDecimal.ZERO) < 0) salePrice = BigDecimal.ZERO;
            } else {
                // PERCENT: giảm theo %
                BigDecimal pct = discount.getValue();
                salePrice = product.getPrice()
                        .multiply(BigDecimal.ONE.subtract(pct.divide(new BigDecimal("100"))))
                        .setScale(2, RoundingMode.HALF_UP);

                // Áp dụng maxDiscount cap nếu có (chỉ áp dụng cho PERCENT)
                if (discount.getMaxDiscount() != null) {
                    BigDecimal saved = product.getPrice().subtract(salePrice);
                    if (saved.compareTo(discount.getMaxDiscount()) > 0) {
                        salePrice = product.getPrice().subtract(discount.getMaxDiscount())
                                .setScale(2, RoundingMode.HALF_UP);
                    }
                }
            }
            return salePrice;
        }
        return product.getPrice();
    }

    /**
     * Tính số tiền được giảm theo unit (PERCENT hoặc AMOUNT) của discount.
     */
    private BigDecimal calculateDiscountAmount(Discount discount, BigDecimal baseAmount) {
        BigDecimal result;
        if (discount.getDiscountUnit() == com.foodshop.enums.DiscountUnit.AMOUNT) {
            // Giảm số tiền cố định, không vượt quá baseAmount
            result = discount.getValue().min(baseAmount).setScale(2, RoundingMode.HALF_UP);
        } else {
            // PERCENT
            result = baseAmount
                    .multiply(discount.getValue().divide(new BigDecimal("100")))
                    .setScale(2, RoundingMode.HALF_UP);
            // Áp dụng cap maxDiscount nếu có
            if (discount.getMaxDiscount() != null
                    && result.compareTo(discount.getMaxDiscount()) > 0) {
                result = discount.getMaxDiscount();
            }
        }
        return result;
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems() == null
                ? List.of()
                : order.getOrderItems().stream()
                        .map(this::toOrderItemResponse)
                        .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .shippingAddress(order.getShippingAddress())
                .shippingNote(order.getShippingNote())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .shippingDiscount(order.getShippingDiscount())
                .finalAmount(order.getFinalAmount())
                .discountCode(order.getDiscountCode())
                .userId(order.getUser() != null ? order.getUser().getUserId() : null)
                .orderItems(itemResponses)
                .build();
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        Product p = item.getProduct();
        String imageUrl = (p.getImages() != null && !p.getImages().isEmpty())
                ? p.getImages().get(0).getImageUrl()
                : null;

        return OrderItemResponse.builder()
                .orderItemId(item.getOrderItemId())
                .productId(p.getProductId())
                .productName(p.getName())
                .productImageUrl(imageUrl)
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }
}
