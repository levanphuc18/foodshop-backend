package com.foodshop.service.implement;

import com.foodshop.service.ShippingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DefaultShippingProvider implements ShippingProvider {

    @Value("${app.shipping.default-fee:30000}")
    private BigDecimal defaultFee;

    @Override
    public BigDecimal calculateFee(BigDecimal subtotalAmount, String shippingAddress) {
        return defaultFee;
    }
}
