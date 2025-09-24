package com.foodshop.mapper;

import com.foodshop.dto.request.ProductRequest;
import com.foodshop.dto.response.ProductResponse;
import com.foodshop.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "discount", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    Product toProduct(ProductRequest request);

    @Mapping(source = "category.categoryId", target = "categoryId")
    @Mapping(source = "discount.discountId", target = "discountId")
    ProductResponse toProductResponse(Product product);

    void updateProductFromRequest(ProductRequest request, @MappingTarget Product product);
}