package com.project.batch_service.domain.settlement;

import com.project.batch_service.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailySettlement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long settlementId;

    private Long sellerId;

    private LocalDate settlementDate;

    private int totalOrderCount;

    private int totalClaimCount;

    private int totalProductCount;

    private int totalQuantity;

    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal salesAmount = BigDecimal.ZERO;
    private BigDecimal promotionDiscountAmount = BigDecimal.ZERO;
    private BigDecimal couponDiscountAmount = BigDecimal.ZERO;
    private BigDecimal pointUsedAmount = BigDecimal.ZERO;
    private BigDecimal shippingFee = BigDecimal.ZERO;
    private BigDecimal claimShippingFee = BigDecimal.ZERO;
    private BigDecimal commissionAmount = BigDecimal.ZERO;
}
