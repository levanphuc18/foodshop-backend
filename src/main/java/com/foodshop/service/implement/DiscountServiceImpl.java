package com.foodshop.service.implement;

import com.foodshop.dto.request.DiscountRequest;
import com.foodshop.dto.response.DiscountResponse;
import com.foodshop.entity.Discount;
import com.foodshop.enums.DiscountType;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.mapper.DiscountMapper;
import com.foodshop.repository.DiscountRepository;
import com.foodshop.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new GlobalException(GlobalCode.DISCOUNT_INVALID_DATE_RANGE);
        }

        if(request.getType() == DiscountType.ORDER){
            if(request.getMinOrderAmount() == null){
                throw new GlobalException(GlobalCode.MIN_ORDER_AMOUNT_REQUIRED);
            }
        } else if(request.getType() == DiscountType.PRODUCT){
            if(request.getMinOrderAmount() != null){
                throw new GlobalException(GlobalCode.INVALID_MIN_ORDER_AMOUNT);
            }
        }

        Discount discount = discountMapper.toDiscount(request);
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
        return discountRepository.findAll().stream()
                .map(discountMapper::toDiscountResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DiscountResponse updateDiscount(Integer id, DiscountRequest request) {
        Discount existing = discountRepository.findById(id)
                .orElseThrow(() -> new GlobalException(GlobalCode.DISCOUNT_NOT_FOUND));

        if (!existing.getCode().equals(request.getCode()) && discountRepository.existsByCode(request.getCode())) {
            throw new GlobalException(GlobalCode.DISCOUNT_CODE_EXISTS);
        }

        if(request.getType() == DiscountType.ORDER){
            if(request.getMinOrderAmount() == null){
                throw new GlobalException(GlobalCode.MIN_ORDER_AMOUNT_REQUIRED);
            }
        } else if(request.getType() == DiscountType.PRODUCT){
            if(request.getMinOrderAmount() != null){
                throw new GlobalException(GlobalCode.INVALID_MIN_ORDER_AMOUNT);
            }
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new GlobalException(GlobalCode.DISCOUNT_INVALID_DATE_RANGE);
        }

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
}