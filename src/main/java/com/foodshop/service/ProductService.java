package com.foodshop.service;

import com.foodshop.dto.request.ProductRequest;
import com.foodshop.dto.response.ProductResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse updateProduct(Integer id, ProductRequest request);
    void deleteProduct(Integer id);
    ProductResponse getProductById(Integer id);
    List<ProductResponse> getAllProducts();
    List<ProductResponse> getProductsByCategory(Integer categoryId);
    Page<ProductResponse> searchProducts(String keyword, int page, int size, boolean asc);
}