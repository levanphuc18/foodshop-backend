package com.foodshop.mapper;

import com.foodshop.dto.request.ProductRequest;
import com.foodshop.dto.response.ProductResponse;
import com.foodshop.entity.Discount;
import com.foodshop.entity.Product;
import com.foodshop.enums.DiscountStatus;
import com.foodshop.enums.DiscountType;
import com.foodshop.enums.DiscountUnit;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "discount", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "isActive", source = "isActive")
    Product toProduct(ProductRequest request);

    @Mapping(source = "category.categoryId", target = "categoryId")
    @Mapping(source = "discount.discountId", target = "discountId")
    @Mapping(target = "imageUrls", expression = "java(mapImageUrls(product))")
    @Mapping(target = "salePrice", expression = "java(computeSalePrice(product))")
    @Mapping(target = "discountPercentage", expression = "java(computeDiscountPercentage(product))")
    @Mapping(target = "discountType", expression = "java(computeDiscountType(product))")
    @Mapping(target = "discountUnit", expression = "java(computeDiscountUnit(product))")
    @Mapping(target = "maxDiscount", expression = "java(computeMaxDiscount(product))")
    @Mapping(target = "productStatus", expression = "java(computeProductStatus(product))")
    @Mapping(target = "isActive", source = "isActive")
    ProductResponse toProductResponse(Product product);

    default List<String> mapImageUrls(Product product) {
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            return product.getImages().stream()
                    .map(img -> img.getImageUrl())
                    .collect(Collectors.toList());
        }
        return null;
    }

    /** Trả về % giảm nếu discount PRODUCT đang active. AMOUNT unit → tính % tương đương. */
    default BigDecimal computeDiscountPercentage(Product product) {
        Discount discount = product.getDiscount();
        if (discount != null
                && product.getPrice() != null
                && discount.getType() == DiscountType.PRODUCT
                && discount.getValue() != null
                && discount.getStatus() == DiscountStatus.ACTIVE
                && !LocalDate.now().isBefore(discount.getStartDate())
                && !LocalDate.now().isAfter(discount.getEndDate())) {

            boolean isPercent = discount.getDiscountUnit() == DiscountUnit.PERCENT || 
                                (discount.getDiscountUnit() == null && discount.getMaxDiscount() != null && discount.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0) ||
                                (discount.getDiscountUnit() == null && discount.getValue().compareTo(new BigDecimal("100")) <= 0);

            if (isPercent) {
                return discount.getValue();
            } else {
                // AMOUNT unit: tính % tương đương để frontend hiển thị badge
                if (product.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                    return discount.getValue()
                            .multiply(new BigDecimal("100"))
                            .divide(product.getPrice(), 2, RoundingMode.HALF_UP);
                }
            }
        }
        return null;
    }

    /** Tính salePrice cho PRODUCT discount (hỗ trợ cả PERCENT và AMOUNT unit). */
    default BigDecimal computeSalePrice(Product product) {
        Discount discount = product.getDiscount();
        if (discount != null
                && product.getPrice() != null
                && discount.getType() == DiscountType.PRODUCT
                && discount.getValue() != null
                && discount.getStatus() == DiscountStatus.ACTIVE
                && !LocalDate.now().isBefore(discount.getStartDate())
                && !LocalDate.now().isAfter(discount.getEndDate())) {

            boolean isPercent = discount.getDiscountUnit() == DiscountUnit.PERCENT || 
                                (discount.getDiscountUnit() == null && discount.getMaxDiscount() != null && discount.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0) ||
                                (discount.getDiscountUnit() == null && discount.getValue().compareTo(new BigDecimal("100")) <= 0);

            BigDecimal salePrice;
            if (!isPercent) {
                salePrice = product.getPrice().subtract(discount.getValue())
                        .setScale(2, RoundingMode.HALF_UP);
                // Không để salePrice âm
                if (salePrice.compareTo(BigDecimal.ZERO) < 0) salePrice = BigDecimal.ZERO;
            } else {
                // PERCENT
                BigDecimal pct = discount.getValue();
                salePrice = product.getPrice()
                        .multiply(BigDecimal.ONE.subtract(pct.divide(new BigDecimal("100"))))
                        .setScale(2, RoundingMode.HALF_UP);
                // Áp dụng maxDiscount cap nếu có (chỉ PERCENT mới hợp lệ)
                if (discount.getMaxDiscount() != null && discount.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal savedAmount = product.getPrice().subtract(salePrice);
                    if (savedAmount.compareTo(discount.getMaxDiscount()) > 0) {
                        salePrice = product.getPrice().subtract(discount.getMaxDiscount())
                                .setScale(2, RoundingMode.HALF_UP);
                    }
                }
            }
            return salePrice;
        }
        return null;
    }

    /** Trả về discount type (String) để frontend biết loại giảm giá đang áp dụng. */
    default String computeDiscountType(Product product) {
        Discount discount = product.getDiscount();
        if (discount != null
                && discount.getType() == DiscountType.PRODUCT
                && discount.getStatus() == DiscountStatus.ACTIVE
                && !LocalDate.now().isBefore(discount.getStartDate())
                && !LocalDate.now().isAfter(discount.getEndDate())) {
            return discount.getType().name();
        }
        return null;
    }

    /** Trả về discount unit (String) để frontend biết hiển thị % hay số tiền. */
    default String computeDiscountUnit(Product product) {
        Discount discount = product.getDiscount();
        if (discount != null
                && discount.getType() == DiscountType.PRODUCT
                && discount.getStatus() == DiscountStatus.ACTIVE
                && !LocalDate.now().isBefore(discount.getStartDate())
                && !LocalDate.now().isAfter(discount.getEndDate())) {
            
            if (discount.getDiscountUnit() != null) return discount.getDiscountUnit().name();
            
            // Legacy data fallback
            boolean isPercent = (discount.getMaxDiscount() != null && discount.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0) ||
                                (discount.getValue().compareTo(new BigDecimal("100")) <= 0);
            return isPercent ? DiscountUnit.PERCENT.name() : DiscountUnit.AMOUNT.name();
        }
        return null;
    }

    /** Trả về giá trị giảm tối đa nếu có. */
    default BigDecimal computeMaxDiscount(Product product) {
        Discount discount = product.getDiscount();
        if (discount != null
                && discount.getType() == DiscountType.PRODUCT
                && discount.getStatus() == DiscountStatus.ACTIVE
                && !LocalDate.now().isBefore(discount.getStartDate())
                && !LocalDate.now().isAfter(discount.getEndDate())) {
            return discount.getMaxDiscount();
        }
        return null;
    }

    default String computeProductStatus(Product product) {
        if (product.getQuantity() == null || product.getQuantity() <= 0) {
            return "OUT_OF_STOCK";
        }
        if (product.getQuantity() <= 10) {
            return "LOW_STOCK";
        }
        return "IN_STOCK";
    }

    @Mapping(target = "isActive", source = "isActive")
    void updateProductFromRequest(ProductRequest request, @MappingTarget Product product);
}
