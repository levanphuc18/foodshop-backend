package com.foodshop.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryResponse {
    private Integer categoryId;
    private String name;
    private String description;
    private String imageUrl;
    private LocalDateTime createdAt;
}