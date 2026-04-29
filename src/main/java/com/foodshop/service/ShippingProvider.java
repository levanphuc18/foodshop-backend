package com.foodshop.service;

import java.math.BigDecimal;

public interface ShippingProvider {
    BigDecimal calculateFee(BigDecimal subtotalAmount, String shippingAddress);
}
