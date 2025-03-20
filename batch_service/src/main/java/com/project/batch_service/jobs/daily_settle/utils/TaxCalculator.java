package com.project.batch_service.jobs.daily_settle.utils;

import com.project.batch_service.domain.products.TaxType;
import com.project.batch_service.jobs.daily_settle.dto.OrderProductDTO;

import java.math.BigDecimal;

public class TaxCalculator {

    private static final BigDecimal DEFAULT_TAX_RATE = BigDecimal.valueOf(0.03);

    private final OrderProductDTO orderProductDTO;

    public TaxCalculator(OrderProductDTO orderProductDTO) {
        this.orderProductDTO = orderProductDTO;
    }

    public BigDecimal getTaxAmount() {
        TaxType taxType = orderProductDTO.getTaxType();

        if (taxType == TaxType.TAXABLE) {
            return calculateTaxByItemCategory();
        } else {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculateTaxByItemCategory() {
        BigDecimal sellPrice = orderProductDTO.getSellPrice();

        // todo : 상품 카테고리에 따른 세금 계산

        return sellPrice
                .multiply(BigDecimal.valueOf(orderProductDTO.getQuantity()))
                .multiply(DEFAULT_TAX_RATE);
    }
}
