package com.foodshop.service;

import com.foodshop.dto.request.CategoryRequest;
import com.foodshop.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest categoryRequest);

    CategoryResponse getCategoryById(Integer categoryId);

    List<CategoryResponse> getAllCategories();

    CategoryResponse updateCategory(Integer categoryId, CategoryRequest categoryRequest);

    void deleteCategory(Integer categoryId);
}