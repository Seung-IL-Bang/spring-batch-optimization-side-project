package com.project.batch_service.domain.settlement;

import com.project.batch_service.domain.BaseEntity;
import com.project.batch_service.domain.products.TaxType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailySettlementDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dailySettlementDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_settlement_id")
    private DailySettlement dailySettlement;

    private Long productId;

    private Long orderProductId;

    private Long orderId;

    private int quantity;

    @Enumerated(EnumType.STRING)
    private SettlementStatus settlementStatus;

    @Enumerated(EnumType.STRING)
    private TaxType taxType;

    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal salesAmount = BigDecimal.ZERO;
    private BigDecimal promotionDiscountAmount = BigDecimal.ZERO;
    private BigDecimal couponDiscountAmount = BigDecimal.ZERO;
    private BigDecimal pointUsedAmount = BigDecimal.ZERO;
    private BigDecimal shippingFee = BigDecimal.ZERO;
    private BigDecimal claimShippingFee = BigDecimal.ZERO;
    private BigDecimal commissionAmount = BigDecimal.ZERO;
    private BigDecimal settlementAmount = BigDecimal.ZERO;

    public void updateDetail(DailySettlementDetail dailySettlementDetail) {

        this.quantity = dailySettlementDetail.getQuantity();
        this.settlementStatus = dailySettlementDetail.getSettlementStatus();
        this.taxAmount = dailySettlementDetail.getTaxAmount();
        this.salesAmount = dailySettlementDetail.getSalesAmount();
        this.promotionDiscountAmount = dailySettlementDetail.getPromotionDiscountAmount();
        this.couponDiscountAmount = dailySettlementDetail.getCouponDiscountAmount();
        this.pointUsedAmount = dailySettlementDetail.getPointUsedAmount();
        this.shippingFee = dailySettlementDetail.getShippingFee();
        this.claimShippingFee = dailySettlementDetail.getClaimShippingFee();
        this.commissionAmount = dailySettlementDetail.getCommissionAmount();
        this.settlementAmount = dailySettlementDetail.getSettlementAmount();
    }
}
