package com.project.batch_service.job.daily_settle;

import com.project.batch_service.domain.products.Products;
import com.project.batch_service.domain.products.TaxType;

import java.math.BigDecimal;

public class TaxCalculator {

    private static final BigDecimal DEFAULT_TAX_RATE = BigDecimal.valueOf(0.03);

    private final Products product;

    public TaxCalculator(Products product) {
        this.product = product;
    }

    public BigDecimal getTaxAmount() {
        TaxType taxType = product.getTaxType();

        if (taxType == TaxType.TAXABLE) {
            return calculateTaxByItemCategory();
        } else {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculateTaxByItemCategory() {
        BigDecimal sellPrice = product.getSellPrice();

        // todo : 상품 카테고리에 따른 세금 계산

        return sellPrice.multiply(DEFAULT_TAX_RATE);
    }
}
