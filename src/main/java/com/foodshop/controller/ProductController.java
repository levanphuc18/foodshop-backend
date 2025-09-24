package com.foodshop.controller;

import com.foodshop.dto.ApiResponse;
import com.foodshop.dto.request.ProductRequest;
import com.foodshop.dto.response.ProductResponse;
import com.foodshop.exception.GlobalCode;
import com.foodshop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/ping")
    public String ping() {
        return "server is running";
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "true") boolean asc
    ) {
        Page<ProductResponse> result = productService.searchProducts(keyword, page, size, asc);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @ModelAttribute @Valid ProductRequest request) {

        ProductResponse response = productService.createProduct(request);

        ApiResponse<ProductResponse> apiResponse = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "Product created successfully.",
                response
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PutMapping("/{id}")
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
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        List<ProductResponse> responses = productService.getAllProducts();

        ApiResponse<List<ProductResponse>> apiResponse = new ApiResponse<>(
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
}