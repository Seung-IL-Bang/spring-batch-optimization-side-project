package com.project.batch_service.job.daily_settle;

import com.project.batch_service.domain.orders.Orders;
import com.project.batch_service.domain.products.Products;
import com.project.batch_service.domain.seller.Seller;

import java.math.BigDecimal;

public class ProductSettlementAmountCalculator {

    private final Products product;
    private final Orders order;
    private final Seller seller;
    private final BigDecimal commission;
    private final BigDecimal taxAmount;

    public ProductSettlementAmountCalculator(Products product, Orders order, Seller seller, BigDecimal commission, BigDecimal taxAmount) {
        this.product = product;
        this.order = order;
        this.seller = seller;
        this.commission = commission;
        this.taxAmount = taxAmount;
    }

    public BigDecimal getProductSettlementAmount() {
        BigDecimal sellPrice = product.getSellPrice();
        return sellPrice
                .subtract(order.getPointUsedAmount())
                .subtract(order.getPromotionDiscountAmount())
                .subtract(order.getCouponDiscountAmount())
                .subtract(seller.getDefaultDeliveryAmount())
                .subtract(commission)
                .subtract(taxAmount);
    }

}
