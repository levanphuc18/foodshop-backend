package com.foodshop.service;

import com.foodshop.dto.request.ProductRequest;
import com.foodshop.dto.response.ProductResponse;
import com.foodshop.dto.request.BulkAssignDiscountRequest;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Integer id, ProductRequest request);

    void bulkAssignDiscount(BulkAssignDiscountRequest request);

    void deleteProduct(Integer id);

    ProductResponse getProductById(Integer id);

    ProductResponse getProductByIdAdmin(Integer id);

    List<ProductResponse> getAllProducts();

    Page<ProductResponse> getAllProducts(
            String search,
            Integer categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    Page<ProductResponse> getAllProductsAdmin(
            String search,
            Integer categoryId,
            String status,
            Boolean isActive,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    List<ProductResponse> getProductsByCategory(Integer categoryId);

    List<ProductResponse> getProductsByCategoryAdmin(Integer categoryId);
}
