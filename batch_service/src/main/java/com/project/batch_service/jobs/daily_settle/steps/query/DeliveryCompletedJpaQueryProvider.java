package com.project.batch_service.jobs.daily_settle.steps.query;

import com.project.batch_service.domain.orders.OrderProduct;
import jakarta.persistence.Query;
import org.springframework.batch.item.database.orm.AbstractJpaQueryProvider;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;

public class DeliveryCompletedJpaQueryProvider extends AbstractJpaQueryProvider {

    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    public DeliveryCompletedJpaQueryProvider(LocalDateTime startTime, LocalDateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    @NonNull
    public Query createQuery() {
        return this.getEntityManager()
                .createQuery(
                         "SELECT op " +
                                "FROM OrderProduct op " +
                                "LEFT JOIN Claim cl ON op.orderProductId = cl.orderProductId " +
                                "WHERE op.deliveryCompletedAt BETWEEN :startTime AND :endTime " +
                                "AND op.deliveryStatus = 'DELIVERED' " +
                                "AND op.purchaseConfirmedAt IS NULL " +
                                "AND (cl.claimId IS NULL OR cl.completedAt IS NOT NULL)", OrderProduct.class)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }
}
