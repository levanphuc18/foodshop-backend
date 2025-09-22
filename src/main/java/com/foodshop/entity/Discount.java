package com.foodshop.entity;

import com.foodshop.enums.DiscountStatus;
import com.foodshop.enums.DiscountType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "discounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class Discount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "discount_id")
    private Integer discountId;

    @NotBlank(message = "Discount code is required")
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @NotNull(message = "Discount type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private DiscountType type;

    @NotNull(message = "Discount value is required")
    @Column(name = "value", nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DiscountStatus status;
}