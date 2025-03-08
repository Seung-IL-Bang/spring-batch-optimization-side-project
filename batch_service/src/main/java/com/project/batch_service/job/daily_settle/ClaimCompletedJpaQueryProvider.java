package com.project.batch_service.job.daily_settle;

import com.project.batch_service.domain.orders.OrderProduct;
import jakarta.persistence.Query;
import org.springframework.batch.item.database.orm.AbstractJpaQueryProvider;

import java.time.LocalDateTime;

public class ClaimCompletedJpaQueryProvider extends AbstractJpaQueryProvider {

    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    public ClaimCompletedJpaQueryProvider(LocalDateTime startTime, LocalDateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public Query createQuery() {
        return this.getEntityManager()
                .createQuery(
                        "SELECT NEW com.project.batch_service.job.daily_settle.ClaimRefundDto(" +
                                "c.claimId, " +
                                "c.claimType, " +
                                "c.claimStatus, " +
                                "c.extraFeePayer, " +
                                "c.completedAt, " +
                                "ops.taxType, " +
                                "crh.refundSalesAmount, " +
                                "crh.refundTaxAmount, " +
                                "crh.refundPromotionAmount, " +
                                "crh.refundCouponAmount, " +
                                "crh.refundPointAmount, " +
                                "crh.refundDeliveryAmount, " +
                                "crh.extraDeliveryAmount, " +
                                "crh.refundCommissionAmount, " +
                                "crh.refundSettlementAmount, " +
                                "op.order.orderId, " +
                                "op.orderProductId, " +
                                "op.product.productId, " +
                                "ops.seller.sellerId, " +
                                "ops.quantity, " +
                                "ops.sellPrice, " +
                                "ops.supplyPrice) " +
                                "FROM Claim c " +
                                "LEFT JOIN ClaimRefundHistory crh ON c.claimId = crh.claimId " +
                                "LEFT JOIN OrderProduct op ON op.orderProductId = c.orderProductId " +
                                "LEFT JOIN OrderProductSnapshot ops ON ops.orderProductSnapshotId = op.orderProductSnapshot.orderProductSnapshotId " +
                                "WHERE c.claimStatus = 'COMPLETED' " +
                                "AND c.claimType = 'REFUND' " +
                                "AND c.completedAt BETWEEN :startTime AND :endTime",
                        ClaimRefundDto.class)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
