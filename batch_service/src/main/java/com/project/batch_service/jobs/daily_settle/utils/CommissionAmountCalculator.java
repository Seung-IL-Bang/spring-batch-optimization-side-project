package com.project.batch_service.jobs.daily_settle.utils;

import com.project.batch_service.jobs.daily_settle.dto.OrderProductDTO;

import java.math.BigDecimal;

public class CommissionAmountCalculator {

    private final OrderProductDTO orderProductDto;

    public CommissionAmountCalculator(OrderProductDTO orderProductDto) {
        this.orderProductDto = orderProductDto;
    }

    public BigDecimal getCommissionAmount() {
        BigDecimal sellPrice = orderProductDto.getSellPrice();
        BigDecimal supplyPrice = orderProductDto.getSupplyPrice();
        int quantity = orderProductDto.getQuantity();
        double commissionRate = orderProductDto.getCommissionRate();
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
