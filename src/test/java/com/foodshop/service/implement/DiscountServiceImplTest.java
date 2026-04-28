package com.foodshop.service.implement;

import com.foodshop.dto.request.DiscountRequest;
import com.foodshop.dto.response.CouponValidationResponse;
import com.foodshop.dto.response.DiscountResponse;
import com.foodshop.entity.Discount;
import com.foodshop.enums.DiscountStatus;
import com.foodshop.enums.DiscountType;
import com.foodshop.enums.DiscountUnit;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.mapper.DiscountMapper;
import com.foodshop.repository.DiscountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountServiceImplTest {

    @Mock
    private DiscountRepository discountRepository;
    @Mock
    private DiscountMapper discountMapper;

    @InjectMocks
    private DiscountServiceImpl discountService;

    @Test
    void validateCouponShouldRejectProductCouponForCheckout() {
        Discount discount = buildDiscount("PRODUCT10", DiscountType.PRODUCT, DiscountUnit.AMOUNT, "10000");
        when(discountRepository.findByCode("PRODUCT10")).thenReturn(Optional.of(discount));

        CouponValidationResponse response = discountService.validateCoupon("PRODUCT10", new BigDecimal("100000"));

        assertFalse(response.isValid());
        assertEquals("PRODUCT10", response.getCode());
    }

    @Test
    void validateCouponShouldRejectWhenOrderAmountBelowMinimum() {
        Discount discount = buildDiscount("ORDER20", DiscountType.ORDER, DiscountUnit.PERCENT, "20");
        discount.setMinOrderAmount(new BigDecimal("500000"));
        when(discountRepository.findByCode("ORDER20")).thenReturn(Optional.of(discount));

        CouponValidationResponse response = discountService.validateCoupon("ORDER20", new BigDecimal("300000"));

        assertFalse(response.isValid());
        assertEquals(new BigDecimal("500000"), response.getMinOrderAmount());
    }

    @Test
    void validateCouponShouldReturnCappedPercentDiscount() {
        Discount discount = buildDiscount("ORDER50", DiscountType.ORDER, DiscountUnit.PERCENT, "50");
        discount.setMinOrderAmount(new BigDecimal("100000"));
        discount.setMaxDiscount(new BigDecimal("60000"));
        when(discountRepository.findByCode("ORDER50")).thenReturn(Optional.of(discount));

        CouponValidationResponse response = discountService.validateCoupon("ORDER50", new BigDecimal("200000"));

        assertTrue(response.isValid());
        assertEquals(new BigDecimal("60000"), response.getDiscountAmount());
        assertEquals(DiscountType.ORDER, response.getType());
    }

    @Test
    void getAllDiscountsShouldOnlyReturnActiveAndCurrentDiscounts() {
        Discount activeCurrent = buildDiscount("ACTIVE1", DiscountType.ORDER, DiscountUnit.AMOUNT, "10000");
        Discount disabled = buildDiscount("DISABLED1", DiscountType.ORDER, DiscountUnit.AMOUNT, "10000");
        disabled.setStatus(DiscountStatus.DISABLED);
        Discount expired = buildDiscount("EXPIRED1", DiscountType.ORDER, DiscountUnit.AMOUNT, "10000");
        expired.setEndDate(LocalDate.now().minusDays(1));

        when(discountRepository.findAll()).thenReturn(List.of(activeCurrent, disabled, expired));
        DiscountResponse mapped = new DiscountResponse();
        mapped.setCode("ACTIVE1");
        when(discountMapper.toDiscountResponse(activeCurrent)).thenReturn(mapped);

        List<DiscountResponse> responses = discountService.getAllDiscounts();

        assertEquals(1, responses.size());
        assertEquals("ACTIVE1", responses.get(0).getCode());
    }

    @Test
    void createDiscountShouldRejectInvalidProductDiscountRequest() {
        DiscountRequest request = new DiscountRequest();
        request.setCode("P10");
        request.setType(DiscountType.PRODUCT);
        request.setDiscountUnit(DiscountUnit.AMOUNT);
        request.setValue(new BigDecimal("10000"));
        request.setMinOrderAmount(new BigDecimal("500000"));
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(1));
        request.setStatus(DiscountStatus.ACTIVE);

        when(discountRepository.existsByCode("P10")).thenReturn(false);

        GlobalException exception = assertThrows(GlobalException.class,
                () -> discountService.createDiscount(request));

        assertEquals(GlobalCode.INVALID_MIN_ORDER_AMOUNT.getCode(), exception.getCode());
    }

    private Discount buildDiscount(String code, DiscountType type, DiscountUnit unit, String value) {
        Discount discount = new Discount();
        discount.setCode(code);
        discount.setType(type);
        discount.setDiscountUnit(unit);
        discount.setValue(new BigDecimal(value));
        discount.setStartDate(LocalDate.now().minusDays(1));
        discount.setEndDate(LocalDate.now().plusDays(1));
        discount.setStatus(DiscountStatus.ACTIVE);
        return discount;
    }
}
