package com.foodshop.controller;

import com.foodshop.dto.ApiResponse;
import com.foodshop.dto.request.CategoryRequest;
import com.foodshop.dto.response.CategoryResponse;
import com.foodshop.exception.GlobalCode;
import com.foodshop.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @ModelAttribute @Valid CategoryRequest categoryRequest) {

        CategoryResponse categoryResponse = categoryService.createCategory(categoryRequest);

        ApiResponse<CategoryResponse> apiResponse = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "Category created successfully.",
                categoryResponse
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Integer id) {
        CategoryResponse categoryResponse = categoryService.getCategoryById(id);
        ApiResponse<CategoryResponse> apiResponse = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "Category retrieved successfully.",
                categoryResponse
        );
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> categoryResponses = categoryService.getAllCategories();
        ApiResponse<List<CategoryResponse>> apiResponse = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "Categories retrieved successfully.",
                categoryResponses
        );
        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Integer id,
            @ModelAttribute @Valid CategoryRequest categoryRequest) {

        CategoryResponse updatedCategory = categoryService.updateCategory(id, categoryRequest);
        ApiResponse<CategoryResponse> apiResponse = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "Category updated successfully.",
                updatedCategory
        );
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Integer id) {
        categoryService.deleteCategory(id);
        ApiResponse<Void> apiResponse = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "Category deleted successfully.",
                null
        );
        return ResponseEntity.ok(apiResponse);
    }
}
