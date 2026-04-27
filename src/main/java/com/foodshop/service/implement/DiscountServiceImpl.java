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

    private final DiscountRepository discountRepository;
    private final DiscountMapper discountMapper;

    @Override
    public DiscountResponse createDiscount(DiscountRequest request) {
        if (discountRepository.existsByCode(request.getCode())) {
            throw new GlobalException(GlobalCode.DISCOUNT_CODE_EXISTS);
        }

        validateDiscountRequest(request);

        Discount discount = discountMapper.toDiscount(request);
        discount = discountRepository.save(discount);

        return discountMapper.toDiscountResponse(discount);
    }

    private void validateDiscountRequest(DiscountRequest request) {
        // Date validation
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new GlobalException(GlobalCode.DISCOUNT_INVALID_DATE_RANGE);
        }

        // Percentage validation: value must be 0-100
        if (request.getDiscountUnit() == com.foodshop.enums.DiscountUnit.PERCENT) {
            if (request.getValue().doubleValue() > 100) {
                throw new GlobalException(GlobalCode.INVALID_PERCENTAGE_VALUE);
            }
        } else {
            // Unit = AMOUNT: maxDiscount không có nghĩa (vì đã là số tiền cố định)
            if (request.getMaxDiscount() != null) {
                throw new GlobalException(GlobalCode.INVALID_MAX_DISCOUNT);
            }
        }

        // Type specific validation
        if (request.getType() == DiscountType.PRODUCT) {
            // Product discount: không dùng minOrderAmount (giảm thẳng vào sản phẩm)
            if (request.getMinOrderAmount() != null) {
                throw new GlobalException(GlobalCode.INVALID_MIN_ORDER_AMOUNT);
            }
            // maxDiscount chỉ hợp lệ khi unit = PERCENT (để cap số tiền tối đa được giảm)
            // (đã validate ở trên: AMOUNT + maxDiscount → lỗi)
        } else {
            // ORDER hoặc SHIPPING: cần minOrderAmount
            if (request.getMinOrderAmount() == null) {
                throw new GlobalException(GlobalCode.MIN_ORDER_AMOUNT_REQUIRED);
            }
        }
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
                .filter(discount -> !today.isBefore(discount.getStartDate()) && !today.isAfter(discount.getEndDate()))
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
    public CouponValidationResponse validateCoupon(String code, BigDecimal totalAmount) {
        // Tìm coupon
        Discount discount = discountRepository.findByCode(code.trim()).orElse(null);
        if (discount == null) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .code(code)
                    .message("Mã giảm giá không tồn tại.")
                    .build();
        }

        // Chỉ ORDER và SHIPPING coupon mới có thể nhập tay khi checkout
        if (discount.getType() == DiscountType.PRODUCT) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .code(code)
                    .message("Mã này chỉ áp dụng trực tiếp cho sản phẩm.")
                    .build();
        }

        // Kiểm tra trạng thái và thời hạn
        LocalDate today = LocalDate.now();
        if (discount.getStatus() != DiscountStatus.ACTIVE
                || today.isBefore(discount.getStartDate())
                || today.isAfter(discount.getEndDate())) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .code(code)
                    .message("Mã giảm giá đã hết hạn hoặc chưa được kích hoạt.")
                    .build();
        }

        // Kiểm tra đơn hàng tối thiểu
        if (discount.getMinOrderAmount() != null
                && totalAmount.compareTo(discount.getMinOrderAmount()) < 0) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .code(code)
                    .minOrderAmount(discount.getMinOrderAmount())
                    .message("Đơn hàng chưa đạt giá trị tối thiểu " + discount.getMinOrderAmount() + "đ để áp dụng mã này.")
                    .build();
        }

        // Nếu đơn hàng đã 0đ, không cho áp dụng mã giảm giá ORDER (vô nghĩa)
        // Nhưng vẫn cho áp dụng mã SHIPPING
        if (discount.getType() == DiscountType.ORDER && totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return CouponValidationResponse.builder()
                    .valid(false)
                    .code(code)
                    .message("Đơn hàng đã là 0đ, không thể áp dụng thêm mã giảm giá đơn hàng.")
                    .build();
        }

        // Tính số tiền được giảm
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (discount.getType() == DiscountType.ORDER) {
            discountAmount = calculateDiscountAmount(discount, totalAmount);
        } else {
            // SHIPPING: giá trị = số tiền phí ship được giảm
            discountAmount = discount.getValue();
        }

        return CouponValidationResponse.builder()
                .valid(true)
                .code(discount.getCode())
                .type(discount.getType())
                .discountUnit(discount.getDiscountUnit())
                .value(discount.getValue())
                .discountAmount(discountAmount)
                .minOrderAmount(discount.getMinOrderAmount())
                .maxDiscount(discount.getMaxDiscount())
                .message(discount.getType() == DiscountType.ORDER
                        ? "Áp dụng thành công! Bạn được giảm " + discountAmount + "đ."
                        : "Áp dụng thành công! Phí ship được giảm " + discountAmount + "đ.")
                .build();
    }

    /** Helper: tính số tiền giảm theo unit (PERCENT hoặc AMOUNT) */
    private BigDecimal calculateDiscountAmount(Discount discount, BigDecimal baseAmount) {
        if (discount.getDiscountUnit() == DiscountUnit.AMOUNT) {
            return discount.getValue().min(baseAmount).setScale(2, RoundingMode.HALF_UP);
        } else {
            BigDecimal result = baseAmount
                    .multiply(discount.getValue().divide(new BigDecimal("100")))
                    .setScale(2, RoundingMode.HALF_UP);
            if (discount.getMaxDiscount() != null && result.compareTo(discount.getMaxDiscount()) > 0) {
                result = discount.getMaxDiscount();
            }
            return result;
        }
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
}
