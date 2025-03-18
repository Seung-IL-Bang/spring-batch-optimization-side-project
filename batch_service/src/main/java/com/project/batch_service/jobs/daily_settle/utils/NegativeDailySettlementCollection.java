package com.project.batch_service.jobs.daily_settle.utils;

import com.project.batch_service.domain.products.TaxType;
import com.project.batch_service.domain.settlement.DailySettlement;
import com.project.batch_service.domain.settlement.DailySettlementDetail;
import com.project.batch_service.domain.settlement.SettlementStatus;
import com.project.batch_service.domain.settlement.repository.DailySettlementRepository;
import com.project.batch_service.jobs.daily_settle.dto.ClaimRefundDto;

import java.time.LocalDate;

public class NegativeDailySettlementCollection {

    private final ClaimRefundDto claimRefundDto;
    private final DailySettlementRepository dailySettlementRepository;
    private final LocalDate settlementDate;

    public NegativeDailySettlementCollection(ClaimRefundDto ClaimRefundDto,
                                             DailySettlementRepository dailySettlementRepository,
                                             LocalDate settlementDate) {
        this.claimRefundDto = ClaimRefundDto;
        this.dailySettlementRepository = dailySettlementRepository;
        this.settlementDate = settlementDate;
    }

    public DailySettlementDetail getDailySettlementDetail() {

        Long sellerId = claimRefundDto.getSellerId();
        TaxType taxType = claimRefundDto.getTaxType();
        Integer quantity = claimRefundDto.getQuantity();
        Long orderProductId = claimRefundDto.getOrderProductId();
        Long orderId = claimRefundDto.getOrderId();
        Long productId = claimRefundDto.getProductId();

        DailySettlement dailySettlement = dailySettlementRepository.findBySellerIdAndSettlementDate(sellerId, settlementDate)
                .orElseThrow(() -> new IllegalArgumentException("DailySettlement is not found"));

        return DailySettlementDetail.builder()
                .dailySettlement(dailySettlement)
                .settlementStatus(SettlementStatus.REFUNDED)
                .orderProductId(orderProductId)
                .orderId(orderId)
                .productId(productId)
                .quantity(quantity)
                .taxType(taxType)
                .salesAmount(claimRefundDto.getRefundSalesAmount())
                .taxAmount(claimRefundDto.getRefundTaxAmount())
                .promotionDiscountAmount(claimRefundDto.getRefundPromotionAmount())
                .couponDiscountAmount(claimRefundDto.getRefundCouponAmount())
                .pointUsedAmount(claimRefundDto.getRefundPointAmount())
                .shippingFee(claimRefundDto.getRefundDeliveryAmount())
                .claimShippingFee(claimRefundDto.getExtraDeliveryAmount())
                .commissionAmount(claimRefundDto.getRefundCommissionAmount())
                .settlementAmount(claimRefundDto.getRefundSettlementAmount())
                .build();
    }
}
