package com.foodshop.controller;

import com.foodshop.dto.ApiResponse;
import com.foodshop.exception.GlobalCode;
import com.foodshop.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payment")
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @GetMapping("/create-url")
    public ResponseEntity<ApiResponse<String>> createPaymentUrl(
            @RequestParam Integer orderId,
            HttpServletRequest request) {
        String paymentUrl = paymentService.createVNPayUrl(orderId, request);
        return ResponseEntity.ok(new ApiResponse<>(GlobalCode.SUCCESS, "URL generated", paymentUrl));
    }

    @GetMapping("/vnpay-return")
    public void vnpayReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean success = paymentService.processVnpayReturn(request);
        if (success) {
            response.sendRedirect(frontendUrl + "/orders?payment_status=success");
        } else {
            response.sendRedirect(frontendUrl + "/orders?payment_status=failed");
        }
    }
}
