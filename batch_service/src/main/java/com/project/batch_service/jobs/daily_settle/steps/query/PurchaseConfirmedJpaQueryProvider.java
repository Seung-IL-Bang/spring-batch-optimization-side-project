package com.project.batch_service.jobs.daily_settle.steps.query;


import com.project.batch_service.domain.orders.DeliveryStatus;
import com.project.batch_service.domain.orders.OrderProduct;
import jakarta.persistence.Query;
import org.springframework.batch.item.database.orm.AbstractJpaQueryProvider;

import java.time.LocalDateTime;

public class PurchaseConfirmedJpaQueryProvider extends AbstractJpaQueryProvider {

    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    public PurchaseConfirmedJpaQueryProvider(LocalDateTime startTime, LocalDateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public Query createQuery() {
        // JPQL에서 JOIN FETCH를 사용하여 연관 엔티티를 한 번에 로딩
        String jpql = """
            SELECT op FROM OrderProduct op
            JOIN FETCH op.orderProductSnapshot
            JOIN FETCH op.order
            JOIN FETCH op.product p
            JOIN FETCH p.seller
            WHERE op.purchaseConfirmedAt BETWEEN :startTime AND :endTime
            AND op.deliveryStatus = 'DELIVERED'
            ORDER BY op.orderProductId ASC
            """;

        return this.getEntityManager()
                .createQuery(jpql, OrderProduct.class)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
