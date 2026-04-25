package com.foodshop.service;

import com.foodshop.dto.request.ProductRequest;
import com.foodshop.dto.response.ProductResponse;
import com.foodshop.dto.request.BulkAssignDiscountRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Integer id, ProductRequest request);

    void bulkAssignDiscount(BulkAssignDiscountRequest request);

    void deleteProduct(Integer id);

    ProductResponse getProductById(Integer id);

    ProductResponse getProductByIdAdmin(Integer id);

    List<ProductResponse> getAllProducts();

    Page<ProductResponse> getAllProductsAdmin(Integer categoryId, int page, int size, boolean asc);

    List<ProductResponse> getProductsByCategory(Integer categoryId);

    List<ProductResponse> getProductsByCategoryAdmin(Integer categoryId);

    Page<ProductResponse> searchProducts(String keyword, Integer categoryId, int page, int size, boolean asc);

    Page<ProductResponse> searchProductsAdmin(String keyword, Integer categoryId, int page, int size, boolean asc);
}
