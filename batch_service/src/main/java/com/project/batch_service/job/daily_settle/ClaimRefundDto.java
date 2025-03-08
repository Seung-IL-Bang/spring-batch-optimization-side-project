package com.project.batch_service.job.daily_settle;

import com.project.batch_service.domain.claims.ClaimStatus;
import com.project.batch_service.domain.claims.ClaimType;
import com.project.batch_service.domain.claims.ExtraFeePayer;
import com.project.batch_service.domain.products.TaxType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ClaimRefundDto {
    private final Long claimId;
    private final ClaimType claimType;
    private final ClaimStatus claimStatus;
    private final ExtraFeePayer extraFeePayer;
    private final LocalDateTime completedAt;
    private final TaxType taxType;
    private final BigDecimal refundSalesAmount;
    private final BigDecimal refundTaxAmount;
    private final BigDecimal refundPromotionAmount;
    private final BigDecimal refundCouponAmount;
    private final BigDecimal refundPointAmount;
    private final BigDecimal refundDeliveryAmount;
    private final BigDecimal extraDeliveryAmount;
    private final BigDecimal refundCommissionAmount;
    private final BigDecimal refundSettlementAmount;
    private final Long orderId;
    private final Long orderProductId;
    private final Long productId;
    private final Long sellerId;
    private final Integer quantity;
    private final BigDecimal sellPrice;
    private final BigDecimal supplyPrice;

}