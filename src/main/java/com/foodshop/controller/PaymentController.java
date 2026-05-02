package com.foodshop.controller;

import com.foodshop.dto.ApiResponse;
import com.foodshop.exception.GlobalCode;
import com.foodshop.service.PaymentService;
import com.foodshop.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    /**
     * SECURITY FIX [P0-IDOR]: Thêm owner check – chỉ chủ order mới được tạo payment URL.
     */
    @GetMapping("/create-url")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> createPaymentUrl(
            @RequestParam Integer orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(GlobalCode.UNAUTHORIZED, "Please log in.", null));
        }

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Integer callerUserId = isAdmin ? null : userDetails.getUser().getUserId();

        String paymentUrl = paymentService.createVNPayUrl(orderId, callerUserId, request);
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
