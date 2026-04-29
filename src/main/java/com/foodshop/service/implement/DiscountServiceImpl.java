package com.foodshop.service.implement;

import com.foodshop.dto.request.DiscountRequest;
import com.foodshop.dto.response.CouponValidationResponse;
import com.foodshop.dto.response.DiscountResponse;
import com.foodshop.entity.Discount;
import com.foodshop.enums.DiscountStatus;
import com.foodshop.enums.DiscountType;
import com.foodshop.enums.DiscountUnit;
import com.foodshop.enums.OrderStatus;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.mapper.DiscountMapper;
import com.foodshop.repository.DiscountRepository;
import com.foodshop.repository.OrderAppliedDiscountRepository;
import com.foodshop.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements DiscountService {

    private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("30000");

    private final DiscountRepository discountRepository;
    private final DiscountMapper discountMapper;
    private final OrderAppliedDiscountRepository orderAppliedDiscountRepository;

    @Override
    public DiscountResponse createDiscount(DiscountRequest request) {
        if (discountRepository.existsByCode(request.getCode())) {
            throw new GlobalException(GlobalCode.DISCOUNT_CODE_EXISTS);
        }

        validateDiscountRequest(request);

        Discount discount = discountMapper.toDiscount(request);
        if (discount.getUsedCount() == null) {
            discount.setUsedCount(0);
        }
        discount = discountRepository.save(discount);

        return discountMapper.toDiscountResponse(discount);
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountResponse getDiscountById(Integer id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new GlobalException(GlobalCode.DISCOUNT_NOT_FOUND));
        return discountMapper.toDiscountResponse(discount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscountResponse> getAllDiscounts() {
        LocalDate today = LocalDate.now();
        return discountRepository.findAll().stream()
                .filter(discount -> discount.getStatus() == DiscountStatus.ACTIVE)
                .filter(discount -> isWithinDateRange(discount, today))
                .filter(this::hasRemainingUsage)
                .map(discountMapper::toDiscountResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiscountResponse> getAllDiscountsAdmin(String keyword, DiscountStatus status, DiscountType type, int page, int size, String sortBy, boolean asc) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        Specification<Discount> specification = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();

            if (!normalizedKeyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + normalizedKeyword + "%"));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, size, resolveDiscountSort(sortBy, asc));
        return discountRepository.findAll(specification, pageable)
                .map(discountMapper::toDiscountResponse);
    }

    @Override
    @Transactional
    public DiscountResponse updateDiscount(Integer id, DiscountRequest request) {
        Discount existing = discountRepository.findById(id)
                .orElseThrow(() -> new GlobalException(GlobalCode.DISCOUNT_NOT_FOUND));

        if (!existing.getCode().equals(request.getCode()) && discountRepository.existsByCode(request.getCode())) {
            throw new GlobalException(GlobalCode.DISCOUNT_CODE_EXISTS);
        }

        validateDiscountRequest(request);
        discountMapper.updateDiscountFromRequest(request, existing);

        return discountMapper.toDiscountResponse(discountRepository.save(existing));
    }

    @Override
    public void deleteDiscount(Integer id) {
        if (!discountRepository.existsById(id)) {
            throw new GlobalException(GlobalCode.DISCOUNT_NOT_FOUND);
        }
        discountRepository.deleteById(id);
    }

    @Override
    @Transactional
    public DiscountResponse toggleDiscountStatus(Integer id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new GlobalException(GlobalCode.DISCOUNT_NOT_FOUND));

        if (discount.getStatus() == DiscountStatus.ACTIVE) {
            discount.setStatus(DiscountStatus.DISABLED);
        } else {
            discount.setStatus(DiscountStatus.ACTIVE);
        }

        return discountMapper.toDiscountResponse(discountRepository.save(discount));
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(String code, BigDecimal totalAmount, Integer userId) {
        String normalizedCode = normalizeCode(code);
        try {
            Discount discount = validateDiscountForCheckout(normalizedCode, totalAmount, userId);
            BigDecimal discountAmount = calculateDiscountAmount(
                    discount,
                    discount.getType() == DiscountType.SHIPPING ? DEFAULT_SHIPPING_FEE : totalAmount
            );

            return CouponValidationResponse.builder()
                    .valid(true)
                    .code(discount.getCode())
                    .type(discount.getType())
                    .discountUnit(discount.getDiscountUnit())
                    .value(discount.getValue())
                    .discountAmount(discountAmount)
                    .minOrderAmount(discount.getMinOrderAmount())
                    .maxDiscount(discount.getMaxDiscount())
                    .message(discount.getType() == DiscountType.SHIPPING
                            ? "Shipping discount is available."
                            : "Coupon applied successfully.")
                    .build();
        } catch (GlobalException ex) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .code(normalizedCode)
                    .message(ex.getMessage())
                    .build();
        }
    }

    public CouponValidationResponse validateCoupon(String code, BigDecimal totalAmount) {
        return validateCoupon(code, totalAmount, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Discount validateDiscountForCheckout(String code, BigDecimal totalAmount, Integer userId) {
        Discount discount = discountRepository.findByCode(normalizeCode(code))
                .orElseThrow(() -> new GlobalException(GlobalCode.DISCOUNT_NOT_FOUND));

        if (discount.getType() == DiscountType.PRODUCT) {
            throw new GlobalException(GlobalCode.DISCOUNT_NOT_VALID, "This code is only for product discounts.");
        }

        if (discount.getStatus() != DiscountStatus.ACTIVE || !isWithinDateRange(discount, LocalDate.now())) {
            throw new GlobalException(GlobalCode.DISCOUNT_NOT_VALID);
        }

        if (!hasRemainingUsage(discount)) {
            throw new GlobalException(GlobalCode.DISCOUNT_USAGE_LIMIT_REACHED);
        }

        if (discount.getMinOrderAmount() != null && totalAmount.compareTo(discount.getMinOrderAmount()) < 0) {
            throw new GlobalException(GlobalCode.DISCOUNT_NOT_APPLICABLE);
        }

        if (discount.getType() == DiscountType.ORDER && totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new GlobalException(GlobalCode.DISCOUNT_NOT_APPLICABLE, "Order amount must be greater than zero.");
        }

        if (userId != null && discount.getPerUserLimit() != null) {
            long activeUsage = orderAppliedDiscountRepository.countActiveUsageByDiscountAndUser(
                    discount.getDiscountId(),
                    userId,
                    OrderStatus.CANCELLED
            );
            if (activeUsage >= discount.getPerUserLimit()) {
                throw new GlobalException(GlobalCode.DISCOUNT_PER_USER_LIMIT_REACHED);
            }
        }

        return discount;
    }

    @Override
    public BigDecimal calculateDiscountAmount(Discount discount, BigDecimal baseAmount) {
        if (discount.getDiscountUnit() == DiscountUnit.AMOUNT) {
            return discount.getValue().min(baseAmount).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal result = baseAmount
                .multiply(discount.getValue().divide(new BigDecimal("100")))
                .setScale(2, RoundingMode.HALF_UP);

        if (discount.getMaxDiscount() != null && result.compareTo(discount.getMaxDiscount()) > 0) {
            result = discount.getMaxDiscount();
        }
        return result;
    }

    @Override
    @Transactional
    public void reserveUsage(Discount discount) {
        if (discount.getUsageLimit() == null) {
            return;
        }

        int usedCount = discount.getUsedCount() == null ? 0 : discount.getUsedCount();
        if (usedCount >= discount.getUsageLimit()) {
            discount.setStatus(DiscountStatus.DISABLED);
            discountRepository.save(discount);
            throw new GlobalException(GlobalCode.DISCOUNT_USAGE_LIMIT_REACHED);
        }

        discount.setUsedCount(usedCount + 1);
        if (discount.getUsedCount() >= discount.getUsageLimit()) {
            discount.setStatus(DiscountStatus.DISABLED);
        }
        discountRepository.save(discount);
    }

    @Override
    @Transactional
    public void releaseUsage(Discount discount) {
        if (discount.getUsageLimit() == null) {
            return;
        }

        int usedCount = discount.getUsedCount() == null ? 0 : discount.getUsedCount();
        discount.setUsedCount(Math.max(0, usedCount - 1));
        if (discount.getStatus() == DiscountStatus.DISABLED
                && hasRemainingUsage(discount)
                && isWithinDateRange(discount, LocalDate.now())) {
            discount.setStatus(DiscountStatus.ACTIVE);
        }
        discountRepository.save(discount);
    }

    private void validateDiscountRequest(DiscountRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new GlobalException(GlobalCode.DISCOUNT_INVALID_DATE_RANGE);
        }

        if (request.getDiscountUnit() == DiscountUnit.PERCENT && request.getValue().compareTo(new BigDecimal("100")) > 0) {
            throw new GlobalException(GlobalCode.INVALID_PERCENTAGE_VALUE);
        }

        if (request.getDiscountUnit() == DiscountUnit.AMOUNT && request.getMaxDiscount() != null) {
            throw new GlobalException(GlobalCode.INVALID_MAX_DISCOUNT);
        }

        if (request.getType() == DiscountType.PRODUCT && request.getMinOrderAmount() != null) {
            throw new GlobalException(GlobalCode.INVALID_MIN_ORDER_AMOUNT);
        }

        if (request.getType() != DiscountType.PRODUCT && request.getMinOrderAmount() == null) {
            throw new GlobalException(GlobalCode.MIN_ORDER_AMOUNT_REQUIRED);
        }
    }

    private boolean isWithinDateRange(Discount discount, LocalDate today) {
        return !today.isBefore(discount.getStartDate()) && !today.isAfter(discount.getEndDate());
    }

    private boolean hasRemainingUsage(Discount discount) {
        return discount.getUsageLimit() == null
                || (discount.getUsedCount() == null ? 0 : discount.getUsedCount()) < discount.getUsageLimit();
    }

    private Sort resolveDiscountSort(String sortBy, boolean asc) {
        String property = switch (sortBy == null ? "" : sortBy) {
            case "endDate" -> "endDate";
            case "value" -> "value";
            case "code" -> "code";
            default -> "startDate";
        };

        return asc ? Sort.by(property).ascending() : Sort.by(property).descending();
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }
}
