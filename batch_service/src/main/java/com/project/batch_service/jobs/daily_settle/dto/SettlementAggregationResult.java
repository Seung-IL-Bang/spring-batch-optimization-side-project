package com.project.batch_service.jobs.daily_settle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class SettlementAggregationResult {

    private Long settlementId;
    private Long sellerId;
    private LocalDate settlementDate;
    private Integer totalOrderCount;
    private Integer totalClaimCount;
    private Integer totalQuantity;
    private BigDecimal totalSalesAmount;
    private BigDecimal totalTaxAmount;
    private BigDecimal totalPromotionDiscountAmount;
    private BigDecimal totalCouponDiscountAmount;
    private BigDecimal totalPointUsedAmount;
    private BigDecimal totalShippingFee;
    private BigDecimal totalClaimShippingFee;
    private BigDecimal totalCommissionAmount;
    private BigDecimal totalSettlementAmount;

}
