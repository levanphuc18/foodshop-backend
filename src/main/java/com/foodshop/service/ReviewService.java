package com.foodshop.service;

import com.foodshop.dto.request.ReviewRequest;
import com.foodshop.dto.response.ReviewResponse;
import com.foodshop.dto.response.ReviewStatusResponse;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface ReviewService {

    /** Create a new review (validates purchase + order status + uniqueness) */
    ReviewResponse createReview(Integer userId, ReviewRequest request);

    /** Update an existing review */
    ReviewResponse updateReview(Integer userId, Integer reviewId, ReviewRequest request);

    /**
     * Get paginated reviews for a product.
     * @param filterRating  nullable — if set, only that star level
     * @param withImages    if true, only reviews that have images
     * @param sortBy        "createdAt" or "rating"
     * @param sortDir       "ASC" or "DESC"
     */
    Page<ReviewResponse> getProductReviews(
            Integer productId, Integer filterRating, boolean withImages,
            int page, int size, String sortBy, String sortDir);

    /** Star-level breakdown for a product: { 1: count, 2: count, ..., 5: count } */
    Map<Integer, Long> getStarBreakdown(Integer productId);

    /** Check review status per order item for a given order */
    ReviewStatusResponse getOrderReviewStatus(Integer userId, Integer orderId);
}
