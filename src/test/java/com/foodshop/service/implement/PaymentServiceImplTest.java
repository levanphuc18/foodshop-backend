package com.foodshop.service.implement;

import com.foodshop.config.VNPayConfig;
import com.foodshop.entity.Order;
import com.foodshop.entity.User;
import com.foodshop.enums.OrderStatus;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.repository.OrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private VNPayConfig vnPayConfig;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void createVNPayUrlShouldRejectNonPendingOrder() {
        Order order = buildOrder(5, OrderStatus.PAID);
        when(orderRepository.findById(5)).thenReturn(Optional.of(order));

        GlobalException exception = assertThrows(GlobalException.class,
                () -> paymentService.createVNPayUrl(5, mock(HttpServletRequest.class)));

        assertEquals(GlobalCode.ORDER_STATUS_INVALID_FOR_PAYMENT.getCode(), exception.getCode());
    }

    @Test
    void processVnpayReturnShouldUpdateOrderStatusWhenSignatureIsValid() {
        Order order = buildOrder(12, OrderStatus.PENDING);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("vnp_TxnRef", "12345678_12");
        request.addParameter("vnp_ResponseCode", "00");

        when(vnPayConfig.getVnp_HashSecret()).thenReturn("secret");
        String signData = "vnp_ResponseCode=00&vnp_TxnRef=12345678_12";
        request.addParameter("vnp_SecureHash", VNPayConfig.hmacSHA512("secret", signData));
        when(orderRepository.findById(12)).thenReturn(Optional.of(order));
        when(orderRepository.saveAndFlush(order)).thenReturn(order);

        boolean success = paymentService.processVnpayReturn(request);

        assertTrue(success);
        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(orderRepository).saveAndFlush(order);
    }

    @Test
    void processVnpayReturnShouldReturnFalseWhenSignatureIsInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("vnp_TxnRef", "12345678_12");
        request.addParameter("vnp_ResponseCode", "00");
        request.addParameter("vnp_SecureHash", "bad-signature");
        when(vnPayConfig.getVnp_HashSecret()).thenReturn("secret");

        boolean success = paymentService.processVnpayReturn(request);

        assertFalse(success);
    }

    private Order buildOrder(int orderId, OrderStatus status) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setStatus(status);
        order.setShippingAddress("123 Main St");
        order.setTotalAmount(new BigDecimal("100000"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingFee(new BigDecimal("30000"));
        order.setShippingDiscount(BigDecimal.ZERO);
        order.setFinalAmount(new BigDecimal("130000"));
        order.setUser(new User());
        order.setOrderItems(java.util.List.of());
        return order;
    }
}
