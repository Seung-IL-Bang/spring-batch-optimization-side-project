package com.project.batch_service.domain.claims;

import com.project.batch_service.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class ClaimRefundHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long claimRefundHistoryId;

    private Long claimId;

    private Long sellerId;

    private BigDecimal refundSalesAmount = BigDecimal.ZERO;

    private BigDecimal refundPromotionAmount = BigDecimal.ZERO;

    private BigDecimal refundCouponAmount = BigDecimal.ZERO;

    private BigDecimal refundPointAmount = BigDecimal.ZERO;

    private BigDecimal refundDeliveryAmount = BigDecimal.ZERO;

    private BigDecimal extraDeliveryAmount = BigDecimal.ZERO;

    private BigDecimal refundCommissionAmount = BigDecimal.ZERO;

    private BigDecimal refundTaxAmount = BigDecimal.ZERO;

    private BigDecimal refundSettlementAmount = BigDecimal.ZERO;

    private LocalDateTime refundAt;

    public void refunded() {
        this.refundAt = LocalDateTime.now();
    }
}
