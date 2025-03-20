package com.project.batch_service.jobs.daily_settle.utils;

import com.project.batch_service.domain.products.TaxType;
import com.project.batch_service.domain.settlement.DailySettlement;
import com.project.batch_service.domain.settlement.DailySettlementDetail;
import com.project.batch_service.domain.settlement.SettlementStatus;
import com.project.batch_service.domain.settlement.repository.DailySettlementRepository;
import com.project.batch_service.jobs.daily_settle.dto.OrderProductDTO;

import java.math.BigDecimal;

public class PositiveDailySettlementCollection {

    private final OrderProductDTO orderProductDto;
    private final DailySettlementRepository dailySettlementRepository;

    public PositiveDailySettlementCollection(OrderProductDTO orderProductDTO, DailySettlementRepository dailySettlementRepository) {
        this.orderProductDto = orderProductDTO;
        this.dailySettlementRepository = dailySettlementRepository;
    }

    public DailySettlementDetail getDailySettlementDetail() {

        Long dailySettlementId = orderProductDto.getDailySettlementId();
        if (dailySettlementId == null) { return null; }

        DailySettlement dailySettlement = dailySettlementRepository.findById(dailySettlementId)
                .orElse(null);
        if (dailySettlement == null) { return null; }

        Long orderProductId = orderProductDto.getOrderProductId();
        Long orderId = orderProductDto.getOrderId();
        Long productId = orderProductDto.getProductId();
        TaxType taxType = orderProductDto.getTaxType();
        int quantity = orderProductDto.getQuantity();

        BigDecimal taxAmount = new TaxCalculator(orderProductDto).getTaxAmount();
        BigDecimal commissionAmount = new CommissionAmountCalculator(orderProductDto).getCommissionAmount();

        BigDecimal sellPrice = orderProductDto.getSellPrice();
        BigDecimal salesAmount = sellPrice.multiply(BigDecimal.valueOf(quantity));

        BigDecimal settlementAmount = new SettlementAmountCalculator(orderProductDto, salesAmount, commissionAmount, taxAmount).getSettlementAmount();

        return DailySettlementDetail.builder()
                .dailySettlement(dailySettlement)
                .settlementStatus(SettlementStatus.COMPLETED)
                .orderProductId(orderProductId)
                .orderId(orderId)
                .productId(productId)
                .quantity(quantity)
                .salesAmount(salesAmount)
                .taxAmount(taxAmount)
                .taxType(taxType)
                .promotionDiscountAmount(orderProductDto.getPromotionDiscountAmount())
                .couponDiscountAmount(orderProductDto.getCouponDiscountAmount())
                .pointUsedAmount(orderProductDto.getPointUsedAmount())
                .shippingFee(orderProductDto.getDefaultDeliveryAmount())
                .claimShippingFee(BigDecimal.ZERO)
                .commissionAmount(commissionAmount)
                .settlementAmount(settlementAmount)
                .build();
    }


}
