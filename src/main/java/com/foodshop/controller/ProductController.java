package com.foodshop.controller;

import com.foodshop.dto.ApiResponse;
import com.foodshop.dto.response.PageResponse;
import com.foodshop.dto.response.ProductResponse;
import com.foodshop.exception.GlobalCode;
import com.foodshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "productId") String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) Boolean asc
    ) {
        PageResponse<ProductResponse> response = PageResponse.from(
                productService.getAllProducts(
                        normalizeSearch(search, keyword),
                        categoryId,
                        minPrice,
                        maxPrice,
                        page,
                        size,
                        sortBy,
                        normalizeSortDir(sortDir, asc)
                )
        );
        return ResponseEntity.ok(
                new ApiResponse<>(GlobalCode.SUCCESS, "Products fetched successfully.", response)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Integer id) {
        ProductResponse response = productService.getProductById(id);
        ApiResponse<ProductResponse> apiResponse = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "Product fetched successfully.",
                response
        );
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "productId") String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) Boolean asc) {
        PageResponse<ProductResponse> responses = PageResponse.from(
                productService.getAllProducts(
                        normalizeSearch(search, keyword),
                        categoryId,
                        minPrice,
                        maxPrice,
                        page,
                        size,
                        sortBy,
                        normalizeSortDir(sortDir, asc)
                )
        );
        ApiResponse<PageResponse<ProductResponse>> apiResponse = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "All products fetched successfully.",
                responses
        );
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByCategory(
            @PathVariable Integer categoryId) {
        List<ProductResponse> products = productService.getProductsByCategory(categoryId);
        ApiResponse<List<ProductResponse>> apiResponse = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "Products fetched successfully.",
                products
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
