package com.project.batch_service.job.daily_settle;

import com.project.batch_service.domain.orders.OrderProduct;
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

    public PositiveDailySettlementCollection(OrderProduct orderProduct, DailySettlementRepository dailySettlementRepository) {
        this.orderProduct = orderProduct;
        this.dailySettlementRepository = dailySettlementRepository;
    }

    public DailySettlementDetail getDailySettlementDetail() {

        LocalDate settlementDate = LocalDate.now(); // todo jop parameter 로 받아오기

        Long orderProductId = orderProduct.getOrderProductId();
        Orders order = orderProduct.getOrder();
        Long orderId = order.getOrderId();
        Products product = orderProduct.getProduct();
        Long productId = product.getProductId();
        Seller seller = product.getSeller();
        int quantity = orderProduct.getQuantity();

        DailySettlement dailySettlement = dailySettlementRepository.findBySellerIdAndSettlementDate(seller.getSellerId(), settlementDate)
                .orElseThrow(() -> new IllegalArgumentException("DailySettlement is not found"));

        // todo : orderProduct snapshot 을 이용하여 정산금액 계산 why??? Products 테이블의 sellPrice 는 변동성이 존재하기 때문.
        BigDecimal taxAmount = new TaxCalculator(product).getTaxAmount();
        BigDecimal commissionAmount = new CommissionAmountCalculator(product).getCommissionAmount();
        BigDecimal productSettlementAmount = new ProductSettlementAmountCalculator(product, order, seller, commissionAmount, taxAmount)
                .getProductSettlementAmount();

        return DailySettlementDetail.builder()
                .dailySettlementId(dailySettlement.getSettlementId())
                .orderProductId(orderProductId)
                .orderId(orderId)
                .productId(productId)
                .quantity(quantity)
                .taxType(product.getTaxType())
                .taxAmount(taxAmount)
                .salesAmount(product.getSellPrice().multiply(BigDecimal.valueOf(quantity))) // todo 검증: order.getPaidPgAmount => product.getSellPrice() * quantity
                .promotionDiscountAmount(order.getPromotionDiscountAmount())
                .couponDiscountAmount(order.getPromotionDiscountAmount())
                .pointUsedAmount(order.getPointUsedAmount())
                .shippingFee(seller.getDefaultDeliveryAmount())
                .claimShippingFee(BigDecimal.ZERO)
                .commissionAmount(commissionAmount)
                .productSettlementAmount(productSettlementAmount)
                .settlementStatus(SettlementStatus.COMPLETED)
                .build();
    }


}
