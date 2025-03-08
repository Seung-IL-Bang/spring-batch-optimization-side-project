package com.project.batch_service.job.daily_settle;

import com.project.batch_service.domain.orders.OrderProductSnapshot;

import java.math.BigDecimal;

public class SettlementAmountCalculator {

    private final OrderProductSnapshot orderProductSnapshot;
    private final BigDecimal salesAmount;
    private final BigDecimal commission;
    private final BigDecimal taxAmount;

    public SettlementAmountCalculator(OrderProductSnapshot orderProductSnapshot,BigDecimal salesAmount, BigDecimal commission, BigDecimal taxAmount) {
        this.orderProductSnapshot = orderProductSnapshot;
        this.salesAmount = salesAmount;
        this.commission = commission;
        this.taxAmount = taxAmount;
    }

    public BigDecimal getSettlementAmount() {
        BigDecimal pointUsedAmount = orderProductSnapshot.getPointUsedAmount();
        BigDecimal promotionDiscountAmount = orderProductSnapshot.getPromotionDiscountAmount();
        BigDecimal couponDiscountAmount = orderProductSnapshot.getCouponDiscountAmount();
        BigDecimal defaultDeliveryAmount = orderProductSnapshot.getDefaultDeliveryAmount();
        return salesAmount
                .subtract(pointUsedAmount)
                .subtract(promotionDiscountAmount)
                .subtract(couponDiscountAmount)
                .subtract(defaultDeliveryAmount)
                .subtract(commission)
                .subtract(taxAmount);
    }

}
