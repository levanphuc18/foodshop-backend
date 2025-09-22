package com.foodshop.controller;

import com.foodshop.dto.ApiResponse;
import com.foodshop.dto.request.DiscountRequest;
import com.foodshop.dto.response.DiscountResponse;
import com.foodshop.exception.GlobalCode;
import com.foodshop.service.DiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/discounts")
public class DiscountController {

    private final DiscountService discountService;

    @GetMapping("/ping")
    public String ping() {
        return "Server is running!";
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DiscountResponse>> createDiscount(
            @Valid @RequestBody DiscountRequest request) {
        DiscountResponse response = discountService.createDiscount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(GlobalCode.SUCCESS, "Discount created successfully.", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DiscountResponse>> getDiscountById(@PathVariable Integer id) {
        DiscountResponse response = discountService.getDiscountById(id);
        return ResponseEntity.ok(new ApiResponse<>(GlobalCode.SUCCESS, "Discount retrieved successfully.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DiscountResponse>>> getAllDiscounts() {
        List<DiscountResponse> responses = discountService.getAllDiscounts();
        return ResponseEntity.ok(new ApiResponse<>(GlobalCode.SUCCESS, "Discounts retrieved successfully.", responses));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DiscountResponse>> updateDiscount(
            @PathVariable Integer id,
            @Valid @RequestBody DiscountRequest request) {
        DiscountResponse response = discountService.updateDiscount(id, request);
        return ResponseEntity.ok(new ApiResponse<>(GlobalCode.SUCCESS, "Discount updated successfully.", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDiscount(@PathVariable Integer id) {
        discountService.deleteDiscount(id);
        return ResponseEntity.ok(new ApiResponse<>(GlobalCode.SUCCESS, "Discount deleted successfully.", null));
    }
}
