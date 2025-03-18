package com.project.batch_service.jobs.daily_settle.utils;

import com.project.batch_service.domain.orders.OrderProduct;
import com.project.batch_service.domain.orders.OrderProductSnapshot;
import com.project.batch_service.domain.orders.Orders;
import com.project.batch_service.domain.products.Products;
import com.project.batch_service.domain.seller.Seller;
import com.project.batch_service.domain.settlement.DailySettlement;
import com.project.batch_service.domain.settlement.DailySettlementDetail;
import com.project.batch_service.domain.settlement.SettlementStatus;
import com.project.batch_service.domain.settlement.repository.DailySettlementRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PositiveDailySettlementCollection {

    private final OrderProduct orderProduct;
    private final DailySettlementRepository dailySettlementRepository;
    private final LocalDate settlementDate;

    public PositiveDailySettlementCollection(OrderProduct orderProduct,
                                             DailySettlementRepository dailySettlementRepository,
                                             LocalDate settlementDate) {
        this.orderProduct = orderProduct;
        this.dailySettlementRepository = dailySettlementRepository;
        this.settlementDate = settlementDate;
    }

    public DailySettlementDetail getDailySettlementDetail() {

        OrderProductSnapshot orderProductSnapshot = orderProduct.getOrderProductSnapshot();

        Long orderProductId = orderProduct.getOrderProductId();
        Orders order = orderProduct.getOrder();
        Long orderId = order.getOrderId();
        Products product = orderProduct.getProduct();
        Long productId = product.getProductId();
        Seller seller = product.getSeller();
        Long sellerId = seller.getSellerId();
        int quantity = orderProductSnapshot.getQuantity();

        DailySettlement dailySettlement = dailySettlementRepository.findBySellerIdAndSettlementDate(sellerId, settlementDate)
                .orElse(null);

        if (dailySettlement == null) { return null; }

        BigDecimal taxAmount = new TaxCalculator(orderProductSnapshot).getTaxAmount();
        BigDecimal commissionAmount = new CommissionAmountCalculator(orderProductSnapshot).getCommissionAmount();
        BigDecimal salesAmount = orderProductSnapshot.getSellPrice().multiply(BigDecimal.valueOf(quantity));
        BigDecimal settlementAmount = new SettlementAmountCalculator(orderProductSnapshot, salesAmount, commissionAmount, taxAmount).getSettlementAmount();

        return DailySettlementDetail.builder()
                .dailySettlement(dailySettlement)
                .settlementStatus(SettlementStatus.COMPLETED)
                .orderProductId(orderProductId)
                .orderId(orderId)
                .productId(productId)
                .quantity(quantity)
                .salesAmount(salesAmount)
                .taxAmount(taxAmount)
                .taxType(orderProductSnapshot.getTaxType())
                .promotionDiscountAmount(orderProductSnapshot.getPromotionDiscountAmount())
                .couponDiscountAmount(orderProductSnapshot.getCouponDiscountAmount())
                .pointUsedAmount(orderProductSnapshot.getPointUsedAmount())
                .shippingFee(orderProductSnapshot.getDefaultDeliveryAmount())
                .claimShippingFee(BigDecimal.ZERO)
                .commissionAmount(commissionAmount)
                .settlementAmount(settlementAmount)
                .build();
    }


}
