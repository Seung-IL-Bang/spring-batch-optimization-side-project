package com.project.batch_service.jobs.daily_settle.utils;

import com.project.batch_service.jobs.daily_settle.dto.OrderProductDTO;

import java.math.BigDecimal;

public class SettlementAmountCalculator {

    private final OrderProductDTO orderProductDto;
    private final BigDecimal salesAmount;
    private final BigDecimal commission;
    private final BigDecimal taxAmount;

    public SettlementAmountCalculator(OrderProductDTO orderProductDto, BigDecimal salesAmount, BigDecimal commission, BigDecimal taxAmount) {
        this.orderProductDto = orderProductDto;
        this.salesAmount = salesAmount;
        this.commission = commission;
        this.taxAmount = taxAmount;
    }

    public BigDecimal getSettlementAmount() {
        BigDecimal pointUsedAmount = orderProductDto.getPointUsedAmount();
        BigDecimal promotionDiscountAmount = orderProductDto.getPromotionDiscountAmount();
        BigDecimal couponDiscountAmount = orderProductDto.getCouponDiscountAmount();
        BigDecimal defaultDeliveryAmount = orderProductDto.getDefaultDeliveryAmount();
        return salesAmount
                .subtract(pointUsedAmount)
                .subtract(promotionDiscountAmount)
                .subtract(couponDiscountAmount)
                .subtract(defaultDeliveryAmount)
                .subtract(commission)
                .subtract(taxAmount);
    }

}
