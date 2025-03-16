package com.project.batch_service.jobs.daily_settle.steps;

import com.project.batch_service.domain.orders.OrderProduct;
import com.project.batch_service.domain.orders.repository.OrderProductRepository;
import com.project.batch_service.jobs.daily_settle.steps.query.DeliveryCompletedJpaQueryProvider;
import com.project.batch_service.jobs.JobParameterUtils;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class PurchaseConfirmStepConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final OrderProductRepository orderProductRepository;

    @Bean
    @StepScope
    public JpaPagingItemReader<OrderProduct> deliveryCompletedJpaItemReader(
            @Value("#{jobParameters['settlementDate']}") String settlementDateStr,
            @Value("#{jobParameters['chunkSize']}") Integer chunkSize) {

        int CHUNK_SIZE = JobParameterUtils.parseChunkSize(chunkSize);
        LocalDate date = JobParameterUtils.parseSettlementDate(settlementDateStr);
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.plusDays(1).atStartOfDay();
        DeliveryCompletedJpaQueryProvider queryProvider = new DeliveryCompletedJpaQueryProvider(startTime, endTime);
        return new JpaPagingItemReaderBuilder<OrderProduct>()
                .name("deliveryCompletedJpaItemReader")
                .pageSize(CHUNK_SIZE)
                .queryProvider(queryProvider)
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    @Bean
    public ItemProcessor<OrderProduct, OrderProduct> purchaseConfirmedItemProcessor() {
        LocalDateTime now = LocalDateTime.now();
        return (orderProduct) -> orderProduct.purchaseConfirm(now);
    }

    @Bean
    public ItemWriter<OrderProduct> purchaseConfirmedItemWriter() {
        return (orderProducts) -> {
            for (OrderProduct orderProduct : orderProducts) {
                orderProductRepository.save(orderProduct);
            }
        };
    }

}
