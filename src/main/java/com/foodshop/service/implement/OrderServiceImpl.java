package com.foodshop.service.implement;

import com.foodshop.dto.request.OrderRequest;
import com.foodshop.dto.response.OrderItemResponse;
import com.foodshop.dto.response.OrderResponse;
import com.foodshop.entity.CartItem;
import com.foodshop.entity.Discount;
import com.foodshop.entity.Order;
import com.foodshop.entity.OrderAppliedDiscount;
import com.foodshop.entity.OrderItem;
import com.foodshop.entity.Product;
import com.foodshop.entity.User;
import com.foodshop.enums.DiscountStatus;
import com.foodshop.enums.DiscountType;
import com.foodshop.enums.OrderStatus;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.repository.CartItemRepository;
import com.foodshop.repository.OrderAppliedDiscountRepository;
import com.foodshop.repository.OrderRepository;
import com.foodshop.repository.ProductRepository;
import com.foodshop.repository.UserRepository;
import com.foodshop.service.DiscountService;
import com.foodshop.service.OrderService;
import com.foodshop.service.ShippingProvider;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderAppliedDiscountRepository orderAppliedDiscountRepository;
    private final DiscountService discountService;
    private final com.foodshop.service.NotificationService notificationService;
    private final NotificationEmailService notificationEmailService;
    private final OrderEmailModelFactory orderEmailModelFactory;
    private final ShippingProvider shippingProvider;

    @Override
    @Transactional
    public OrderResponse createOrder(Integer userId, OrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(GlobalCode.USER_NOT_FOUND));

        List<CartItem> cartItems = cartItemRepository.findByUser_UserId(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new GlobalException(GlobalCode.CART_NOT_ITEM);
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (product.getQuantity() < cartItem.getQuantity()) {
                throw new GlobalException(GlobalCode.INSUFFICIENT_STOCK);
            }

            BigDecimal snapshotPrice = resolveSalePrice(product);
            BigDecimal subtotal = snapshotPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(snapshotPrice);
            orderItem.setOriginalPrice(product.getPrice());
            orderItem.setSubtotal(subtotal);
            orderItems.add(orderItem);

            totalAmount = totalAmount.add(subtotal);

            product.setQuantity(product.getQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        List<String> requestedCodes = normalizeDiscountCodes(request);
        Discount orderDiscount = null;
        Discount shippingDiscount = null;
        List<Discount> validatedDiscounts = new ArrayList<>();

        for (String code : requestedCodes) {
            Discount discount = discountService.validateDiscountForCheckout(code, totalAmount, userId);
            validatedDiscounts.add(discount);
            if (discount.getType() == DiscountType.ORDER) {
                if (orderDiscount != null) {
                    throw new GlobalException(GlobalCode.DISCOUNT_STACKING_NOT_ALLOWED, "Only one ORDER coupon can be applied per order.");
                }
                orderDiscount = discount;
            } else if (discount.getType() == DiscountType.SHIPPING) {
                if (shippingDiscount != null) {
                    throw new GlobalException(GlobalCode.DISCOUNT_STACKING_NOT_ALLOWED, "Only one SHIPPING coupon can be applied per order.");
                }
                shippingDiscount = discount;
            }
        }

        BigDecimal baseShippingFee = shippingProvider.calculateFee(totalAmount, request.getShippingAddress())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal orderDiscountAmount = orderDiscount == null
                ? BigDecimal.ZERO
                : discountService.calculateDiscountAmount(orderDiscount, totalAmount);
        BigDecimal shippingDiscountAmount = shippingDiscount == null
                ? BigDecimal.ZERO
                : discountService.calculateDiscountAmount(shippingDiscount, baseShippingFee);
        if (shippingDiscountAmount.compareTo(baseShippingFee) > 0) {
            shippingDiscountAmount = baseShippingFee;
        }

        BigDecimal finalTotalAmount = totalAmount.subtract(orderDiscountAmount);
        if (finalTotalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalTotalAmount = BigDecimal.ZERO;
        }

        BigDecimal finalShippingFee = baseShippingFee.subtract(shippingDiscountAmount);
        if (finalShippingFee.compareTo(BigDecimal.ZERO) < 0) {
            finalShippingFee = BigDecimal.ZERO;
        }

        BigDecimal finalAmount = finalTotalAmount.add(finalShippingFee).setScale(2, RoundingMode.HALF_UP);

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingNote(request.getShippingNote());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(orderDiscountAmount);
        order.setShippingFee(baseShippingFee);
        order.setShippingDiscount(shippingDiscountAmount);
        order.setFinalAmount(finalAmount);
        order.setDiscount(orderDiscount);
        order.setDiscountCode(requestedCodes.isEmpty() ? null : String.join(", ", requestedCodes));

        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }
        order.setOrderItems(orderItems);

        List<OrderAppliedDiscount> appliedDiscounts = buildAppliedDiscounts(order, validatedDiscounts, totalAmount, baseShippingFee);
        order.setAppliedDiscounts(appliedDiscounts);

        for (Discount discount : validatedDiscounts) {
            discountService.reserveUsage(discount);
        }

        Order saved = orderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            notificationEmailService.sendOrderCreatedEmail(orderEmailModelFactory.buildForCreated(saved));
        }

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
    public Page<OrderResponse> getAllOrdersAdmin(String keyword, OrderStatus status, int page, int size, String sortBy, boolean asc) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        Specification<Order> specification = (root, query, cb) -> {
            query.distinct(true);

            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            var userJoin = root.join("user", JoinType.LEFT);

            if (!normalizedKeyword.isBlank()) {
                String pattern = "%" + normalizedKeyword + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("shippingAddress")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("discountCode"), "")), pattern),
                        cb.like(cb.lower(userJoin.get("username")), pattern),
                        cb.like(cb.lower(userJoin.get("fullName")), pattern),
                        cb.like(root.get("orderId").as(String.class), pattern)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, size, resolveOrderSort(sortBy, asc));
        return orderRepository.findAll(specification, pageable)
                .map(this::toOrderResponse);
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
        if (currentStatus == OrderStatus.COMPLETED || currentStatus == OrderStatus.CANCELLED) {
            throw new GlobalException(GlobalCode.BAD_REQUEST, "Cannot change the status of a finished order.");
        }

        boolean isValid = switch (currentStatus) {
            case PENDING -> List.of(OrderStatus.PAID, OrderStatus.CONFIRMED, OrderStatus.CANCELLED).contains(newStatus);
            case PAID -> List.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED).contains(newStatus);
            case CONFIRMED -> List.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED).contains(newStatus);
            case SHIPPED -> List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED).contains(newStatus);
            default -> false;
        };

        if (!isValid && currentStatus != newStatus) {
            throw new GlobalException(GlobalCode.BAD_REQUEST,
                    "Invalid status transition from " + currentStatus + " to " + newStatus + ".");
        }

        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        if (currentStatus != newStatus && newStatus == OrderStatus.CANCELLED) {
            restoreDiscountUsage(savedOrder);
        }

        if (currentStatus != newStatus) {
            notificationService.notifyOrderStatusChanged(savedOrder);
        }
        return toOrderResponse(savedOrder);
    }

    private void restoreDiscountUsage(Order order) {
        List<OrderAppliedDiscount> appliedDiscounts = order.getAppliedDiscounts();
        if (appliedDiscounts == null || appliedDiscounts.isEmpty()) {
            return;
        }

        for (OrderAppliedDiscount appliedDiscount : appliedDiscounts) {
            if (appliedDiscount.getDiscount() != null) {
                discountService.releaseUsage(appliedDiscount.getDiscount());
            }
        }
    }

    private List<OrderAppliedDiscount> buildAppliedDiscounts(Order order, List<Discount> discounts, BigDecimal totalAmount, BigDecimal shippingFee) {
        List<OrderAppliedDiscount> appliedDiscounts = new ArrayList<>();
        for (Discount discount : discounts) {
            BigDecimal baseAmount = discount.getType() == DiscountType.SHIPPING ? shippingFee : totalAmount;
            appliedDiscounts.add(OrderAppliedDiscount.builder()
                    .order(order)
                    .discount(discount)
                    .code(discount.getCode())
                    .type(discount.getType())
                    .discountUnit(discount.getDiscountUnit())
                    .value(discount.getValue())
                    .appliedAmount(discountService.calculateDiscountAmount(discount, baseAmount))
                    .build());
        }
        return appliedDiscounts;
    }

    private List<String> normalizeDiscountCodes(OrderRequest request) {
        Set<String> codes = new LinkedHashSet<>();
        if (request.getDiscountCodes() != null) {
            request.getDiscountCodes().stream()
                    .map(this::normalizeCode)
                    .filter(code -> !code.isBlank())
                    .forEach(codes::add);
        }
        if (codes.isEmpty() && request.getDiscountCode() != null && !request.getDiscountCode().isBlank()) {
            codes.add(normalizeCode(request.getDiscountCode()));
        }
        return new ArrayList<>(codes);
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

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
                salePrice = product.getPrice().subtract(discount.getValue())
                        .setScale(2, RoundingMode.HALF_UP);
                if (salePrice.compareTo(BigDecimal.ZERO) < 0) {
                    salePrice = BigDecimal.ZERO;
                }
            } else {
                BigDecimal pct = discount.getValue();
                salePrice = product.getPrice()
                        .multiply(BigDecimal.ONE.subtract(pct.divide(new BigDecimal("100"))))
                        .setScale(2, RoundingMode.HALF_UP);

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

    private Sort resolveOrderSort(String sortBy, boolean asc) {
        String property = switch (sortBy == null ? "" : sortBy) {
            case "finalAmount" -> "finalAmount";
            case "status" -> "status";
            default -> "createdAt";
        };

        return asc ? Sort.by(property).ascending() : Sort.by(property).descending();
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems() == null
                ? List.of()
                : order.getOrderItems().stream()
                .map(this::toOrderItemResponse)
                .collect(Collectors.toList());

        List<String> appliedCodes = order.getAppliedDiscounts() == null
                ? List.of()
                : order.getAppliedDiscounts().stream()
                .map(OrderAppliedDiscount::getCode)
                .toList();

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
                .discountCodes(appliedCodes)
                .userId(order.getUser() != null ? order.getUser().getUserId() : null)
                .username(order.getUser() != null ? order.getUser().getUsername() : null)
                .fullName(order.getUser() != null ? order.getUser().getFullName() : null)
                .orderItems(itemResponses)
                .build();
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        Product product = item.getProduct();
        String imageUrl = (product.getImages() != null && !product.getImages().isEmpty())
                ? product.getImages().get(0).getImageUrl()
                : null;

        return OrderItemResponse.builder()
                .orderItemId(item.getOrderItemId())
                .productId(product.getProductId())
                .productName(product.getName())
                .productImageUrl(imageUrl)
                .price(item.getPrice())
                .originalPrice(item.getOriginalPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }
}
