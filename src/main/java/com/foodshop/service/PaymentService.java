package com.foodshop.service;

import jakarta.servlet.http.HttpServletRequest;

public interface PaymentService {
    String createVNPayUrl(Integer orderId, Integer callerUserId, HttpServletRequest request);

    boolean processVnpayReturn(HttpServletRequest request);
}