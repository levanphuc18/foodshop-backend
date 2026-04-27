package com.foodshop.controller;

import com.foodshop.dto.ApiResponse;
import com.foodshop.dto.request.BulkAssignDiscountRequest;
import com.foodshop.dto.request.ProductRequest;
import com.foodshop.dto.response.PageResponse;
import com.foodshop.dto.response.ProductResponse;
import com.foodshop.exception.GlobalCode;
import com.foodshop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping("/get/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Integer id) {
        ProductResponse response = productService.getProductByIdAdmin(id);
        ApiResponse<ProductResponse> apiResponse = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "Admin product details fetched successfully.",
                response
        );
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAllProductsAdmin(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "productId") String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) Boolean asc) {
        ApiResponse<PageResponse<ProductResponse>> apiResponse = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "Admin product list fetched successfully.",
                PageResponse.from(productService.getAllProductsAdmin(
                        normalizeSearch(search, keyword),
                        categoryId,
                        status,
                        isActive,
                        page,
                        size,
                        sortBy,
                        normalizeSortDir(sortDir, asc)
                ))
        );
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProductsAdmin(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "productId") String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) Boolean asc
    ) {
        ApiResponse<PageResponse<ProductResponse>> apiResponse = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "Admin products fetched successfully.",
                PageResponse.from(productService.getAllProductsAdmin(
                        normalizeSearch(search, keyword),
                        categoryId,
                        status,
                        isActive,
                        page,
                        size,
                        sortBy,
                        normalizeSortDir(sortDir, asc)
                ))
        );
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByCategoryAdmin(
            @PathVariable Integer categoryId) {
        List<ProductResponse> products = productService.getProductsByCategoryAdmin(categoryId);
        ApiResponse<List<ProductResponse>> apiResponse = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "Admin products by category fetched successfully.",
                products
        );
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @ModelAttribute @Valid ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        ApiResponse<ProductResponse> apiResponse = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "Product created successfully.",
                response
        );
        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Integer id,
            @ModelAttribute @Valid ProductRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        ApiResponse<ProductResponse> apiResponse = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "Product updated successfully.",
                response
        );
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
        ApiResponse<Void> apiResponse = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "Product deleted successfully.",
                null
        );
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/assign-discount")
    public ResponseEntity<ApiResponse<Void>> bulkAssignDiscount(
            @RequestBody @Valid BulkAssignDiscountRequest request) {
        productService.bulkAssignDiscount(request);
        ApiResponse<Void> apiResponse = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "Discounts assigned successfully.",
                null
        );
        return ResponseEntity.ok(apiResponse);
    }

    private String normalizeSearch(String search, String keyword) {
        return search != null ? search : keyword;
    }

    private String normalizeSortDir(String sortDir, Boolean asc) {
        if (sortDir != null && !sortDir.isBlank()) {
            return sortDir.toUpperCase(Locale.ROOT);
        }
        return Boolean.TRUE.equals(asc) ? "ASC" : "DESC";
    }
}
