package com.project.batch_service.job.daily_settle;

import com.project.batch_service.domain.orders.Orders;
import com.project.batch_service.domain.products.Products;
import com.project.batch_service.domain.seller.Seller;

import java.math.BigDecimal;

public class ProductSettlementAmountCalculator {

    private final Products product;
    private final Orders order;
    private final Seller seller;

    public ProductSettlementAmountCalculator(Products product, Orders order, Seller seller) {
        this.product = product;
        this.order = order;
        this.seller = seller;
    }

    public BigDecimal getProductSettlementAmount() {
        BigDecimal sellPrice = product.getSellPrice();
        return sellPrice
                .subtract(order.getPointUsedAmount())
                .subtract(order.getPromotionDiscountAmount())
                .subtract(order.getCouponDiscountAmount())
                .subtract(seller.getDefaultDeliveryAmount());
    }

}
