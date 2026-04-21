package com.foodshop.mapper;

import com.foodshop.dto.request.ProductRequest;
import com.foodshop.dto.response.ProductResponse;
import com.foodshop.entity.Discount;
import com.foodshop.entity.Product;
import com.foodshop.enums.DiscountType;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "discount", ignore = true)
    @Mapping(target = "images", ignore = true)
    Product toProduct(ProductRequest request);

    @Mapping(source = "category.categoryId", target = "categoryId")
    @Mapping(source = "discount.discountId", target = "discountId")
    @Mapping(target = "imageUrls", expression = "java(mapImageUrls(product))")
    @Mapping(target = "salePrice", expression = "java(computeSalePrice(product))")
    @Mapping(target = "discountPercentage", expression = "java(computeDiscountPercentage(product))")
    ProductResponse toProductResponse(Product product);

    default List<String> mapImageUrls(Product product) {
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            return product.getImages().stream()
                    .map(img -> img.getImageUrl())
                    .collect(Collectors.toList());
        }
        return null;
    }

    default BigDecimal computeDiscountPercentage(Product product) {
        Discount discount = product.getDiscount();
        if (discount != null && product.getPrice() != null
                && discount.getType() == DiscountType.PRODUCT
                && discount.getValue() != null) {
            return discount.getValue();
        }
        return null;
    }

    default BigDecimal computeSalePrice(Product product) {
        Discount discount = product.getDiscount();
        if (discount != null && product.getPrice() != null
                && discount.getType() == DiscountType.PRODUCT
                && discount.getValue() != null) {
            BigDecimal pct = discount.getValue();
            return product.getPrice()
                    .multiply(BigDecimal.ONE.subtract(pct.divide(new BigDecimal("100"))))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return null;
    }

    void updateProductFromRequest(ProductRequest request, @MappingTarget Product product);
}