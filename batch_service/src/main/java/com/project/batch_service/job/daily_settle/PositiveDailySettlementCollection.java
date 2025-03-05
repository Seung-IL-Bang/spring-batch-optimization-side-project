package com.project.batch_service.job.daily_settle;

import com.project.batch_service.domain.orders.OrderProduct;
import com.project.batch_service.domain.orders.Orders;
import com.project.batch_service.domain.products.Products;
import com.project.batch_service.domain.seller.Seller;
import com.project.batch_service.domain.settlement.DailySettlementDetail;
import com.project.batch_service.domain.settlement.SettlementStatus;

import java.math.BigDecimal;

public class PositiveDailySettlementCollection {

    private final OrderProduct orderProduct;

    public PositiveDailySettlementCollection(OrderProduct orderProduct) {
        this.orderProduct = orderProduct;
    }

    public DailySettlementDetail getDailySettlementDetail() {

        Long orderProductId = orderProduct.getOrderProductId();
        Orders order = orderProduct.getOrder();
        Long orderId = order.getOrderId();
        Products product = orderProduct.getProduct();
        Long productId = product.getProductId();
        Seller seller = product.getSeller();
        int quantity = orderProduct.getQuantity();

        // todo : orderProduct snapshot 을 이용하여 정산금액 계산 why??? Products 테이블의 sellPrice 는 변동성이 존재하기 때문.
        BigDecimal taxAmount = new TaxCalculator(product).getTaxAmount();
        BigDecimal productSettlementAmount = new ProductSettlementAmountCalculator(product, order, seller).getProductSettlementAmount();
        BigDecimal commissionAmount = new CommissionAmountCalculator(product).getCommissionAmount();

        return DailySettlementDetail.builder()
                .orderProductId(orderProductId)
                .orderId(orderId)
                .productId(productId)
                .quantity(quantity)
                .taxType(product.getTaxType())
                .taxAmount(taxAmount)
                .salesAmount(order.getPaidPgAmount())
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
