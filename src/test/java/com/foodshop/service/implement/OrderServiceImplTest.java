package com.foodshop.service.implement;

import com.foodshop.dto.request.OrderRequest;
import com.foodshop.dto.response.OrderResponse;
import com.foodshop.entity.CartItem;
import com.foodshop.entity.CartItemId;
import com.foodshop.entity.Discount;
import com.foodshop.entity.Order;
import com.foodshop.entity.Product;
import com.foodshop.entity.User;
import com.foodshop.enums.DiscountStatus;
import com.foodshop.enums.DiscountType;
import com.foodshop.enums.DiscountUnit;
import com.foodshop.enums.OrderStatus;
import com.foodshop.enums.Role;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.repository.CartItemRepository;
import com.foodshop.repository.DiscountRepository;
import com.foodshop.repository.OrderRepository;
import com.foodshop.repository.ProductRepository;
import com.foodshop.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private DiscountRepository discountRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(10);
        user.setUsername("customer01");
        user.setFullName("Customer One");
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
    }

    @Test
    void updateOrderStatusShouldPersistValidTransition() {
        Order order = buildOrder(OrderStatus.PENDING);
        when(orderRepository.findById(5)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(5, OrderStatus.PAID);

        assertEquals(OrderStatus.PAID, response.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderStatusShouldRejectFinishedOrder() {
        Order order = buildOrder(OrderStatus.COMPLETED);
        when(orderRepository.findById(5)).thenReturn(Optional.of(order));

        GlobalException exception = assertThrows(GlobalException.class,
                () -> orderService.updateOrderStatus(5, OrderStatus.CANCELLED));

        assertEquals(GlobalCode.BAD_REQUEST.getCode(), exception.getCode());
    }

    @Test
    void updateOrderStatusShouldRejectInvalidTransition() {
        Order order = buildOrder(OrderStatus.PENDING);
        when(orderRepository.findById(5)).thenReturn(Optional.of(order));

        GlobalException exception = assertThrows(GlobalException.class,
                () -> orderService.updateOrderStatus(5, OrderStatus.SHIPPED));

        assertEquals(GlobalCode.BAD_REQUEST.getCode(), exception.getCode());
    }

    @Test
    void createOrderShouldApplyOrderDiscountCapAndClearCart() {
        Product product = Product.builder()
                .productId(100)
                .name("Salmon")
                .price(new BigDecimal("100000"))
                .quantity(5)
                .images(List.of())
                .isActive(true)
                .build();

        CartItem cartItem = new CartItem(new CartItemId(10, 100), user, product, 2);
        Discount discount = new Discount();
        discount.setCode("ORDER50");
        discount.setType(DiscountType.ORDER);
        discount.setDiscountUnit(DiscountUnit.PERCENT);
        discount.setValue(new BigDecimal("50"));
        discount.setMaxDiscount(new BigDecimal("60000"));
        discount.setMinOrderAmount(new BigDecimal("100000"));
        discount.setStartDate(LocalDate.now().minusDays(1));
        discount.setEndDate(LocalDate.now().plusDays(1));
        discount.setStatus(DiscountStatus.ACTIVE);

        OrderRequest request = new OrderRequest();
        request.setShippingAddress("123 Main St");
        request.setShippingNote("Call first");
        request.setDiscountCode("ORDER50");

        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUser_UserId(10)).thenReturn(List.of(cartItem));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(discountRepository.findByCode("ORDER50")).thenReturn(Optional.of(discount));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setOrderId(77);
            return saved;
        });

        OrderResponse response = orderService.createOrder(10, request);

        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(new BigDecimal("200000"), response.getTotalAmount());
        assertEquals(new BigDecimal("60000"), response.getDiscountAmount());
        assertEquals(new BigDecimal("30000"), response.getShippingFee());
        assertEquals(new BigDecimal("0"), response.getShippingDiscount());
        assertEquals(new BigDecimal("170000.00"), response.getFinalAmount());
        assertEquals("ORDER50", response.getDiscountCode());
        assertEquals(3, product.getQuantity());
        assertNotNull(response.getOrderItems());
        assertEquals(1, response.getOrderItems().size());
        verify(cartItemRepository).deleteAll(List.of(cartItem));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals(discount, orderCaptor.getValue().getDiscount());
    }

    @Test
    void createOrderShouldRejectWhenStockIsInsufficient() {
        Product product = Product.builder()
                .productId(100)
                .name("Salmon")
                .price(new BigDecimal("100000"))
                .quantity(1)
                .images(List.of())
                .isActive(true)
                .build();
        CartItem cartItem = new CartItem(new CartItemId(10, 100), user, product, 2);
        OrderRequest request = new OrderRequest();
        request.setShippingAddress("123 Main St");

        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(cartItemRepository.findByUser_UserId(10)).thenReturn(List.of(cartItem));

        GlobalException exception = assertThrows(GlobalException.class,
                () -> orderService.createOrder(10, request));

        assertEquals(GlobalCode.INSUFFICIENT_STOCK.getCode(), exception.getCode());
    }

    private Order buildOrder(OrderStatus status) {
        Order order = new Order();
        order.setOrderId(5);
        order.setStatus(status);
        order.setShippingAddress("123 Main St");
        order.setTotalAmount(new BigDecimal("100000"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingFee(new BigDecimal("30000"));
        order.setShippingDiscount(BigDecimal.ZERO);
        order.setFinalAmount(new BigDecimal("130000"));
        order.setUser(user);
        order.setOrderItems(List.of());
        return order;
    }
}
