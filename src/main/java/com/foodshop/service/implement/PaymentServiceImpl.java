package com.foodshop.service.implement;

import com.foodshop.config.VNPayConfig;
import com.foodshop.entity.Order;
import com.foodshop.enums.OrderStatus;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.repository.OrderRepository;
import com.foodshop.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TimeZone;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final VNPayConfig vnPayConfig;
    private final OrderRepository orderRepository;

    /**
     * SECURITY FIX [P0-IDOR]: callerUserId = null nếu ADMIN, ngược lại phải là chủ order.
     */
    @Override
    @Transactional(readOnly = true)
    public String createVNPayUrl(Integer orderId, Integer callerUserId, HttpServletRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new GlobalException(GlobalCode.ORDER_NOT_FOUND));

        // Owner check: CUSTOMER chỉ được tạo URL cho order của chính mình
        if (callerUserId != null && !callerUserId.equals(order.getUser().getUserId())) {
            throw new GlobalException(GlobalCode.ORDER_NOT_FOUND); // 404 – không lộ sự tồn tại
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new GlobalException(GlobalCode.ORDER_STATUS_INVALID_FOR_PAYMENT);
        }

        Map<String, String> vnpParams = buildVnPayParams(order, request);
        String queryUrl = buildQueryUrl(vnpParams);
        String vnpSecureHash = VNPayConfig.hmacSHA512(vnPayConfig.getVnp_HashSecret(), queryUrl);

        return vnPayConfig.getVnp_PayUrl() + "?" + queryUrl + "&vnp_SecureHash=" + vnpSecureHash;
    }

    @Override
    @Transactional
    public boolean processVnpayReturn(HttpServletRequest request) {
        Map<String, String> fields = extractVnPayFields(request);
        String vnpSecureHash = request.getParameter("vnp_SecureHash");

        String signData = buildQueryUrl(fields);
        if (!signData.isEmpty()
                && VNPayConfig.hmacSHA512(vnPayConfig.getVnp_HashSecret(), signData).equalsIgnoreCase(vnpSecureHash)) {
            if ("00".equals(request.getParameter("vnp_ResponseCode"))) {
                return updateOrderStatusToPaid(request.getParameter("vnp_TxnRef"));
            }
        }
        return false;
    }

    private Map<String, String> buildVnPayParams(Order order, HttpServletRequest request) {
        BigDecimal amount = order.getFinalAmount().multiply(new BigDecimal("100"));
        Map<String, String> params = new HashMap<>();

        params.put("vnp_Version", vnPayConfig.getVnp_Version());
        params.put("vnp_Command", vnPayConfig.getVnp_Command());
        params.put("vnp_TmnCode", vnPayConfig.getVnp_TmnCode());
        params.put("vnp_Amount", String.valueOf(amount.longValue()));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_TxnRef", VNPayConfig.getRandomNumber(8) + "_" + order.getOrderId());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderId());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnPayConfig.getVnp_ReturnUrl());
        params.put("vnp_IpAddr", VNPayConfig.getIpAddress(request));

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        cld.add(Calendar.MINUTE, 15);
        params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        return params;
    }

    private String buildQueryUrl(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringJoiner sj = new StringJoiner("&");
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                sj.add(URLEncoder.encode(fieldName, StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
            }
        }
        return sj.toString();
    }

    private Map<String, String> extractVnPayFields(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty() && !fieldName.startsWith("vnp_SecureHash")) {
                fields.put(fieldName, fieldValue);
            }
        }
        return fields;
    }

    private boolean updateOrderStatusToPaid(String txnRef) {
        if (txnRef == null || !txnRef.contains("_")) {
            return false;
        }

        try {
            Integer orderId = Integer.parseInt(txnRef.split("_")[1]);
            return orderRepository.findById(orderId)
                    .map(order -> {
                        if (order.getStatus() == OrderStatus.PENDING) {
                            order.setStatus(OrderStatus.PAID);
                            orderRepository.saveAndFlush(order);
                            return true;
                        }
                        return false;
                    }).orElse(false);
        } catch (Exception e) {
            return false;
        }
    }
}
