package com.foodshop.service.implement;

import com.foodshop.dto.request.CategoryRequest;
import com.foodshop.dto.response.CategoryResponse;
import com.foodshop.entity.Category;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.repository.CategoryRepository;
import com.foodshop.service.CategoryService;
import com.foodshop.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {
        if (categoryRepository.existsByName(categoryRequest.getName())) {
            throw new GlobalException(GlobalCode.CATEGORY_NAME_EXISTS);
        }
        String imageUrl = cloudinaryService.uploadFile(categoryRequest.getImageFile());

//        // test @Transactional
//        if (true) {
//            throw new GlobalException(GlobalCode.CATEGORY_NAME_EXISTS);
//        }

        Category category = new Category();
        category.setName(categoryRequest.getName());
        category.setDescription(categoryRequest.getDescription());
        category.setImageUrl(imageUrl);
        category = categoryRepository.save(category);

        return mapToCategoryResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Integer categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new GlobalException(GlobalCode.CATEGORY_NOT_FOUND));
        return mapToCategoryResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Integer categoryId, CategoryRequest categoryRequest) {
        Category existingCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new GlobalException(GlobalCode.CATEGORY_NOT_FOUND));

        if (!existingCategory.getName().equals(categoryRequest.getName()) && categoryRepository.existsByName(categoryRequest.getName())) {
            throw new GlobalException(GlobalCode.CATEGORY_NAME_EXISTS);
        }
        existingCategory.setName(categoryRequest.getName());
        existingCategory.setDescription(categoryRequest.getDescription());

        if (categoryRequest.getImageFile() != null && !categoryRequest.getImageFile().isEmpty()) {
            String newImageUrl = cloudinaryService.uploadFile(categoryRequest.getImageFile());
            existingCategory.setImageUrl(newImageUrl);
        }

        existingCategory = categoryRepository.save(existingCategory);
        return mapToCategoryResponse(existingCategory);
    }

    @Override
    public void deleteCategory(Integer categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new GlobalException(GlobalCode.CATEGORY_NOT_FOUND);
        }
        categoryRepository.deleteById(categoryId);
    }

    private CategoryResponse mapToCategoryResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setCategoryId(category.getCategoryId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setImageUrl(category.getImageUrl());
        response.setCreatedAt(category.getCreatedAt());
        return response;
    }
}