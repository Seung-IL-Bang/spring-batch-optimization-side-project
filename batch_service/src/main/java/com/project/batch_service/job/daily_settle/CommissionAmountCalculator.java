package com.project.batch_service.job.daily_settle;

import com.project.batch_service.domain.orders.OrderProductSnapshot;
import com.project.batch_service.domain.seller.Seller;

import java.math.BigDecimal;

public class CommissionAmountCalculator {

    private final OrderProductSnapshot orderProductSnapshot;

    public CommissionAmountCalculator(OrderProductSnapshot orderProductSnapshot) {
        this.orderProductSnapshot = orderProductSnapshot;
    }

    public BigDecimal getCommissionAmount() {
        BigDecimal sellPrice = orderProductSnapshot.getSellPrice();
        BigDecimal supplyPrice = orderProductSnapshot.getSupplyPrice();
        int quantity = orderProductSnapshot.getQuantity();
        Seller seller = orderProductSnapshot.getSeller();
        double commissionRate = seller.getCommissionRate();
        BigDecimal marginUnit = sellPrice.subtract(supplyPrice);
        BigDecimal marginAmount = marginUnit.multiply(BigDecimal.valueOf(quantity));
        return calculateCommissionAmount(marginAmount, commissionRate);
    }

    private BigDecimal calculateCommissionAmount(BigDecimal marginAmount, double commissionRate) {

        if (commissionRate == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal rate = BigDecimal.valueOf(commissionRate / 100);

        return marginAmount.multiply(rate);
    }
}
