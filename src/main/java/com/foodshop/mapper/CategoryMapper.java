package com.foodshop.mapper;

import com.foodshop.dto.response.CategoryResponse;
import com.foodshop.entity.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponse toCategoryResponse(Category category);
}
