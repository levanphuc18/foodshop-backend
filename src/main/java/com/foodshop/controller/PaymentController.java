package com.foodshop.controller;

import com.foodshop.dto.ApiResponse;
import com.foodshop.exception.GlobalCode;
import com.foodshop.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payment")
public class PaymentController {

    private final PaymentService paymentService;

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
        // Chuyển hướng người dùng về Frontend sau khi VNPay xử lý xong
        if (success) {
            response.sendRedirect("http://localhost:3000/orders?payment_status=success");
        } else {
            response.sendRedirect("http://localhost:3000/orders?payment_status=failed");
        }
    }
}