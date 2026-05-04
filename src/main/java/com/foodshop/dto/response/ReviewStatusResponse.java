package com.foodshop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatusResponse {
    private Integer orderId;
    private List<OrderItemReviewStatus> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemReviewStatus {
        private Integer orderItemId;
        private Integer productId;
        private String productName;
        private String productImageUrl;
        private boolean reviewed;
        private ReviewResponse review;
    }
}
