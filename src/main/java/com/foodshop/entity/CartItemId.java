package com.foodshop.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class CartItemId implements Serializable {
    private Integer userId;
    private Integer productId;
}