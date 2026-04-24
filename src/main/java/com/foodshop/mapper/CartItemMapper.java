package com.foodshop.mapper;

import com.foodshop.dto.response.CartItemResponse;
import com.foodshop.entity.CartItem;
import org.mapstruct.*;


@Mapper(componentModel = "spring")
public interface CartItemMapper {

    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "product.productId", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.price", target = "productPrice")
    @Mapping(source = "quantity", target = "quantity")
    @Mapping(target = "productImageUrl", expression = "java(mapProductImageUrl(item))")
    CartItemResponse toCartItemResponse(CartItem item);

    default String mapProductImageUrl(CartItem item) {
        if (item.getProduct() != null
                && item.getProduct().getImages() != null
                && !item.getProduct().getImages().isEmpty()) {
            return item.getProduct().getImages().get(0).getImageUrl();
        }
        return null;
    }
}