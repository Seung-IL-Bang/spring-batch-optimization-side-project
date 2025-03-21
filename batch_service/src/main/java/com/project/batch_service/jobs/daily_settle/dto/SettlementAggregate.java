package com.project.batch_service.jobs.daily_settle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class SettlementAggregate {

    private final Long settlementId;
    private final Long sellerId;
    private final LocalDate settlementDate;
    private final String settlementStatus;
    private final int quantity;
    private final BigDecimal taxAmount;
    private final BigDecimal salesAmount;
    private final BigDecimal promotionDiscountAmount;
    private final BigDecimal couponDiscountAmount;
    private final BigDecimal pointUsedAmount;
    private final BigDecimal shippingFee;
    private final BigDecimal claimShippingFee;
    private final BigDecimal commissionAmount;
    private final BigDecimal settlementAmount;

}
