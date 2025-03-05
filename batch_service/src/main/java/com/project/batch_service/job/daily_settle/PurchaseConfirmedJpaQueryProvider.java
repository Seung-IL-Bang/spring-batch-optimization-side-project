package com.project.batch_service.job.daily_settle;


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
        return this.getEntityManager()
                .createQuery(
                 "SELECT op " +
                        "FROM OrderProduct op " +
                        "WHERE op.purchaseConfirmedAt BETWEEN :startTime AND :endTime",
                        OrderProduct.class)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
