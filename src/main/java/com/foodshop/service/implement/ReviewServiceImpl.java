package com.foodshop.service.implement;

import com.foodshop.dto.request.ReviewRequest;
import com.foodshop.dto.response.ReviewResponse;
import com.foodshop.dto.response.ReviewStatusResponse;
import com.foodshop.entity.*;
import com.foodshop.enums.OrderStatus;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.repository.OrderRepository;
import com.foodshop.repository.ProductRepository;
import com.foodshop.repository.ReviewRepository;
import com.foodshop.service.ReviewService;
import com.foodshop.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CloudinaryService cloudinaryService;

    // ─── TAO DANH GIA ───────────────────────────────────────────────────
    @Override
    @Transactional
    public ReviewResponse createReview(Integer userId, ReviewRequest request) {
        // 1. Tai don hang va kiem tra quyen so huu
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new GlobalException(GlobalCode.ORDER_NOT_FOUND));

        if (!order.getUser().getUserId().equals(userId)) {
            throw new GlobalException(GlobalCode.REVIEW_NOT_PURCHASED);
        }

        // 2. Kiem tra trang thai don hang phai la COMPLETED
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new GlobalException(GlobalCode.REVIEW_ORDER_NOT_COMPLETED);
        }

        // 3. Kiem tra order item co thuoc don hang nay va khop voi san pham khong
        OrderItem targetItem = order.getOrderItems().stream()
                .filter(oi -> oi.getOrderItemId().equals(request.getOrderItemId()))
                .findFirst()
                .orElseThrow(() -> new GlobalException(GlobalCode.REVIEW_NOT_PURCHASED,
                        "Order item not found in this order."));

        if (!targetItem.getProduct().getProductId().equals(request.getProductId())) {
            throw new GlobalException(GlobalCode.REVIEW_NOT_PURCHASED,
                    "Product ID does not match the order item.");
        }

        // 4. Kiem tra tinh duy nhat
        if (reviewRepository.existsByUser_UserIdAndOrderItem_OrderItemId(userId, request.getOrderItemId())) {
            throw new GlobalException(GlobalCode.REVIEW_ALREADY_EXISTS);
        }

        // 5. Tao entity danh gia
        Product product = targetItem.getProduct();

        Review review = Review.builder()
                .user(order.getUser())
                .product(product)
                .order(order)
                .orderItem(targetItem)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        // 6. Dinh kem hinh anh neu co
        if (request.getImageUrls() != null) {
            for (String url : request.getImageUrls()) {
                ReviewImage img = ReviewImage.builder()
                        .review(review)
                        .imageUrl(url)
                        .build();
                review.getImages().add(img);
            }
        }
        if (request.getImageFiles() != null && !request.getImageFiles().isEmpty()) {
            for (MultipartFile file : request.getImageFiles()) {
                if (file != null && !file.isEmpty()) {
                    String url = cloudinaryService.uploadFile(file);
                    ReviewImage img = ReviewImage.builder()
                            .review(review)
                            .imageUrl(url)
                            .build();
                    review.getImages().add(img);
                }
            }
        }

        reviewRepository.save(review);

        // 7. Cap nhat thong ke san pham
        refreshProductRatingCache(product.getProductId());

        log.info("Review created: userId={}, productId={}, orderId={}, rating={}",
                userId, product.getProductId(), order.getOrderId(), request.getRating());

        return toResponse(review);
    }

    // ─── CAP NHAT DANH GIA ──────────────────────────────────────────────
    @Override
    @Transactional
    public ReviewResponse updateReview(Integer userId, Integer reviewId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new GlobalException(GlobalCode.REVIEW_NOT_FOUND));

        if (!review.getUser().getUserId().equals(userId)) {
            throw new GlobalException(GlobalCode.FORBIDDEN, "You can only edit your own reviews.");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        // Thay the hinh anh cu bang hinh anh moi
        review.getImages().clear();
        reviewRepository.saveAndFlush(review);

        if (request.getImageUrls() != null) {
            for (String url : request.getImageUrls()) {
                ReviewImage img = ReviewImage.builder()
                        .review(review)
                        .imageUrl(url)
                        .build();
                review.getImages().add(img);
            }
        }
        if (request.getImageFiles() != null && !request.getImageFiles().isEmpty()) {
            for (MultipartFile file : request.getImageFiles()) {
                if (file != null && !file.isEmpty()) {
                    String url = cloudinaryService.uploadFile(file);
                    ReviewImage img = ReviewImage.builder()
                            .review(review)
                            .imageUrl(url)
                            .build();
                    review.getImages().add(img);
                }
            }
        }

        reviewRepository.save(review);

        // Cap nhat lai thong ke cua san pham
        refreshProductRatingCache(review.getProduct().getProductId());

        log.info("Review updated: reviewId={}, userId={}", reviewId, userId);
        return toResponse(review);
    }

    // ─── LAY DANH SACH DANH GIA THEO SAN PHAM ───────────────────────────
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProductReviews(
            Integer productId, Integer filterRating, boolean withImages,
            int page, int size, String sortBy, String sortDir) {

        String effectiveSortBy = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";
        Sort sort = "ASC".equalsIgnoreCase(sortDir)
                ? Sort.by(effectiveSortBy).ascending()
                : Sort.by(effectiveSortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Review> reviews;

        if (filterRating != null && withImages) {
            reviews = reviewRepository.findByProductIdAndRatingWithImages(productId, filterRating, pageable);
        } else if (filterRating != null) {
            reviews = reviewRepository.findByProduct_ProductIdAndRating(productId, filterRating, pageable);
        } else if (withImages) {
            reviews = reviewRepository.findByProductIdWithImages(productId, pageable);
        } else {
            reviews = reviewRepository.findByProduct_ProductId(productId, pageable);
        }

        return reviews.map(this::toResponse);
    }

    // ─── THONG KE SO SAO ────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Long> getStarBreakdown(Integer productId) {
        // Khoi tao mac dinh tat ca so sao bang 0
        Map<Integer, Long> breakdown = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) {
            breakdown.put(i, 0L);
        }
        for (Object[] row : reviewRepository.countByProductGroupByRating(productId)) {
            Integer star = (Integer) row[0];
            Long count = (Long) row[1];
            breakdown.put(star, count);
        }
        return breakdown;
    }

    // ─── TRANG THAI DANH GIA CUA DON HANG ───────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ReviewStatusResponse getOrderReviewStatus(Integer userId, Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new GlobalException(GlobalCode.ORDER_NOT_FOUND));

        if (!order.getUser().getUserId().equals(userId)) {
            throw new GlobalException(GlobalCode.FORBIDDEN, "You can only check your own orders.");
        }

        // Tai tat ca danh gia cua don hang nay vao mot Map voi key la orderItemId
        Map<Integer, Review> reviewMap = reviewRepository.findByOrder_OrderId(orderId).stream()
                .collect(Collectors.toMap(
                        r -> r.getOrderItem().getOrderItemId(),
                        Function.identity()
                ));

        List<ReviewStatusResponse.OrderItemReviewStatus> itemStatuses = new ArrayList<>();
        for (OrderItem item : order.getOrderItems()) {
            Review review = reviewMap.get(item.getOrderItemId());
            String imageUrl = null;
            if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
                imageUrl = item.getProduct().getImages().get(0).getImageUrl();
            }

            itemStatuses.add(ReviewStatusResponse.OrderItemReviewStatus.builder()
                    .orderItemId(item.getOrderItemId())
                    .productId(item.getProduct().getProductId())
                    .productName(item.getProduct().getName())
                    .productImageUrl(imageUrl)
                    .reviewed(review != null)
                    .review(review != null ? toResponse(review) : null)
                    .build());
        }

        return ReviewStatusResponse.builder()
                .orderId(orderId)
                .items(itemStatuses)
                .build();
    }

    // ─── HAM HO TRO ─────────────────────────────────────────────────────

    private void refreshProductRatingCache(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new GlobalException(GlobalCode.PRODUCT_NOT_FOUND));

        Double avg = reviewRepository.computeAverageRating(productId);
        long total = reviewRepository.countByProduct_ProductId(productId);

        product.setAverageRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
        product.setTotalReviews((int) total);
        productRepository.save(product);
    }

    private ReviewResponse toResponse(Review review) {
        User user = review.getUser();
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .maskedName(maskName(user.getFullName() != null ? user.getFullName() : user.getUsername()))
                .productId(review.getProduct().getProductId())
                .productName(review.getProduct().getName())
                .orderId(review.getOrder().getOrderId())
                .orderItemId(review.getOrderItem().getOrderItemId())
                .rating(review.getRating())
                .comment(review.getComment())
                .imageUrls(review.getImages().stream()
                        .map(ReviewImage::getImageUrl)
                        .collect(Collectors.toList()))
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    /** Che giau ten cho muc dich rieng tu: "Nguyen Van A" → "Ng***n A" */
    private String maskName(String name) {
        if (name == null || name.length() <= 2) return name;
        String[] parts = name.split("\\s+");
        if (parts.length == 0) return name;
        String first = parts[0];
        if (first.length() <= 2) return name;
        String masked = first.substring(0, 2) + "***" + first.charAt(first.length() - 1);
        if (parts.length > 1) {
            masked += " " + parts[parts.length - 1];
        }
        return masked;
    }
}
