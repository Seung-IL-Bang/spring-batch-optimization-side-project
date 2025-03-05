package com.project.batch_service.job.daily_settle;

import com.project.batch_service.domain.products.Products;
import com.project.batch_service.domain.seller.Seller;

import java.math.BigDecimal;

public class CommissionAmountCalculator {

    private final Products product;

    public CommissionAmountCalculator(Products product) {
        this.product = product;
    }

    public BigDecimal getCommissionAmount() {
        BigDecimal sellPrice = product.getSellPrice();
        BigDecimal supplyPrice = product.getSupplyPrice();
        Seller seller = product.getSeller();
        double commissionRate = seller.getCommissionRate();
        BigDecimal marginAmount = sellPrice.subtract(supplyPrice);
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
