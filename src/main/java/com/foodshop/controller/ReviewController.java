package com.foodshop.controller;

import com.foodshop.dto.ApiResponse;
import com.foodshop.dto.request.ReviewRequest;
import com.foodshop.dto.response.PageResponse;
import com.foodshop.dto.response.ReviewResponse;
import com.foodshop.dto.response.ReviewStatusResponse;
import com.foodshop.exception.GlobalCode;
import com.foodshop.security.CustomUserDetails;
import com.foodshop.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * User tao danh gia moi cho san pham trong don hang.
     * POST /api/v1/reviews
     */
    @PostMapping(value = "/reviews", consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute @Valid ReviewRequest request) {

        Integer userId = userDetails.getUser().getUserId();
        ReviewResponse response = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(GlobalCode.SUCCESS, "Review created successfully.", response));
    }

    /**
     * User cap nhat danh gia cua minh.
     * PUT /api/v1/reviews/{reviewId}
     */
    @PutMapping(value = "/reviews/{reviewId}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer reviewId,
            @ModelAttribute @Valid ReviewRequest request) {

        Integer userId = userDetails.getUser().getUserId();
        ReviewResponse response = reviewService.updateReview(userId, reviewId, request);
        return ResponseEntity.ok(
                new ApiResponse<>(GlobalCode.SUCCESS, "Review updated successfully.", response));
    }

    /**
     * Lay danh sach danh gia cua mot san pham (Public).
     * GET /api/v1/products/{productId}/reviews
     */
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getProductReviews(
            @PathVariable Integer productId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "false") boolean withImages,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Page<ReviewResponse> reviewPage = reviewService.getProductReviews(
                productId, rating, withImages, page, size, sortBy, sortDir);

        PageResponse<ReviewResponse> pageResponse = PageResponse.from(reviewPage);
        return ResponseEntity.ok(
                new ApiResponse<>(GlobalCode.SUCCESS, "Product reviews fetched successfully.", pageResponse));
    }

    /**
     * Lay thong ke so sao cua mot san pham (Public).
     * GET /api/v1/products/{productId}/reviews/breakdown
     */
    @GetMapping("/products/{productId}/reviews/breakdown")
    public ResponseEntity<ApiResponse<Map<Integer, Long>>> getStarBreakdown(
            @PathVariable Integer productId) {

        Map<Integer, Long> breakdown = reviewService.getStarBreakdown(productId);
        return ResponseEntity.ok(
                new ApiResponse<>(GlobalCode.SUCCESS, "Star breakdown fetched successfully.", breakdown));
    }

    /**
     * Lay trang thai danh gia cac san pham trong don hang cua User.
     * GET /api/v1/orders/{orderId}/reviews-status
     */
    @GetMapping("/orders/{orderId}/reviews-status")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReviewStatusResponse>> getOrderReviewStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer orderId) {

        Integer userId = userDetails.getUser().getUserId();
        ReviewStatusResponse response = reviewService.getOrderReviewStatus(userId, orderId);
        return ResponseEntity.ok(
                new ApiResponse<>(GlobalCode.SUCCESS, "Review status fetched successfully.", response));
    }
}
