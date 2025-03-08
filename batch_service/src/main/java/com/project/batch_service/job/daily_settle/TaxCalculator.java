package com.project.batch_service.job.daily_settle;

import com.project.batch_service.domain.orders.OrderProductSnapshot;
import com.project.batch_service.domain.products.Products;
import com.project.batch_service.domain.products.TaxType;

import java.math.BigDecimal;

public class TaxCalculator {

    private static final BigDecimal DEFAULT_TAX_RATE = BigDecimal.valueOf(0.03);

    private final OrderProductSnapshot orderProductSnapshot;

    public TaxCalculator(OrderProductSnapshot orderProductSnapshot) {
        this.orderProductSnapshot = orderProductSnapshot;
    }

    public BigDecimal getTaxAmount() {
        TaxType taxType = orderProductSnapshot.getTaxType();

        if (taxType == TaxType.TAXABLE) {
            return calculateTaxByItemCategory();
        } else {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculateTaxByItemCategory() {
        BigDecimal sellPrice = orderProductSnapshot.getSellPrice();

        // todo : 상품 카테고리에 따른 세금 계산

        return sellPrice
                .multiply(BigDecimal.valueOf(orderProductSnapshot.getQuantity()))
                .multiply(DEFAULT_TAX_RATE);
    }
}
