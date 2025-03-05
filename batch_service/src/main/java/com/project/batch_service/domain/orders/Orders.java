package com.project.batch_service.domain.orders;

import com.project.batch_service.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
public class Orders extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    private Long customerId;

    private BigDecimal paidPgAmount = BigDecimal.ZERO;

    private BigDecimal promotionDiscountAmount = BigDecimal.ZERO;

    private BigDecimal pointUsedAmount = BigDecimal.ZERO;

    private BigDecimal couponDiscountAmount = BigDecimal.ZERO;

    private LocalDateTime paidConfirmedAt;
}
