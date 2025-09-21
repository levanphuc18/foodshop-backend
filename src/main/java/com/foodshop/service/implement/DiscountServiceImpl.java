package com.foodshop.service.implement;

import com.foodshop.dto.request.DiscountRequest;
import com.foodshop.dto.response.DiscountResponse;
import com.foodshop.entity.Discount;
import com.foodshop.enums.DiscountType;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
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
        }else if(request.getType() == DiscountType.PRODUCT){
            if(request.getMinOrderAmount() != null){
                throw new GlobalException(GlobalCode.INVALID_MIN_ORDER_AMOUNT);
            }
        }

        Discount discount = mapToEntity(request);
        discount = discountRepository.save(discount);
        return mapToResponse(discount);
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountResponse getDiscountById(Integer id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new GlobalException(GlobalCode.DISCOUNT_NOT_FOUND));
        return mapToResponse(discount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscountResponse> getAllDiscounts() {
        return discountRepository.findAll().stream()
                .map(this::mapToResponse)
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

        existing.setCode(request.getCode());
        existing.setType(request.getType());
        existing.setValue(request.getValue());
        existing.setMinOrderAmount(request.getMinOrderAmount());
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());
        existing.setStatus(request.getStatus());

        return mapToResponse(discountRepository.save(existing));
    }


    @Override
    public void deleteDiscount(Integer id) {
        if (!discountRepository.existsById(id)) {
            throw new GlobalException(GlobalCode.DISCOUNT_NOT_FOUND);
        }
        discountRepository.deleteById(id);
    }

    private Discount mapToEntity(DiscountRequest request) {
        Discount d = new Discount();
        d.setCode(request.getCode());
        d.setType(request.getType());
        d.setValue(request.getValue());
        d.setMinOrderAmount(request.getMinOrderAmount());
        d.setStartDate(request.getStartDate());
        d.setEndDate(request.getEndDate());
        d.setStatus(request.getStatus());
        return d;
    }

    private DiscountResponse mapToResponse(Discount discount) {
        DiscountResponse r = new DiscountResponse();
        r.setDiscountId(discount.getDiscountId());
        r.setCode(discount.getCode());
        r.setType(discount.getType());
        r.setValue(discount.getValue());
        r.setMinOrderAmount(discount.getMinOrderAmount());
        r.setStartDate(discount.getStartDate());
        r.setEndDate(discount.getEndDate());
        r.setStatus(discount.getStatus());
        return r;
    }
}
