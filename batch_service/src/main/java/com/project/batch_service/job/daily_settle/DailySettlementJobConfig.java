package com.project.batch_service.job.daily_settle;

import com.project.batch_service.domain.orders.OrderProduct;
import com.project.batch_service.domain.orders.repository.OrderProductRepository;
import com.project.batch_service.domain.settlement.DailySettlement;
import com.project.batch_service.domain.settlement.DailySettlementDetail;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class DailySettlementJobConfig {

    private static final int CHUNK_SIZE = 1000;
    public static final String DAILY_SETTLEMENT_JOB = "dailySettlementJob";
    public static final String PURCHASE_CONFIRMED_STEP = DAILY_SETTLEMENT_JOB + "_purchaseConfirmedStep";
    public static final String PLUS_SETTLEMENT_STEP = DAILY_SETTLEMENT_JOB + "_plusSettlementStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final OrderProductRepository orderProductRepository;

    @Bean
    public Job dailySettlementJob() {
        return new JobBuilder(DAILY_SETTLEMENT_JOB, jobRepository)
                .start(purchaseConfirmedStep())
//                .next(plusSettlementStep())
//                .next(minusSettlementStep())
                .build();
        //todo createDailySettlementStep
    }

    @Bean
    public Step purchaseConfirmedStep() {
        return new StepBuilder(PURCHASE_CONFIRMED_STEP, jobRepository)
                .<OrderProduct, OrderProduct>chunk(CHUNK_SIZE, transactionManager)
                .reader(deliveryCompletedJpaItemReader())
                .processor(purchaseConfirmedItemProcessor())
                .writer(purchaseConfirmedItemWriter())
                .build();
    }

    @Bean
    public Step plusSettlementStep() {
        return new StepBuilder(PLUS_SETTLEMENT_STEP, jobRepository)
                .<OrderProduct, DailySettlementDetail>chunk(CHUNK_SIZE, transactionManager)
                .reader(dailyPlusSettlementJpaItemReader())
//                .processor()
//                .writer()
                .build();
    }

    @Bean
    public Step minusSettlementStep() {
        return null;
    }


    // === purchaseConfirmedStep ===
    @Bean
    public JpaPagingItemReader<OrderProduct> deliveryCompletedJpaItemReader() {
        LocalDate now = LocalDate.now();
        LocalDateTime startTime = now.atStartOfDay();
        LocalDateTime endTime = now.plusDays(1).atStartOfDay();
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

    // === plusSettlementStep ===
    @Bean
    public JpaPagingItemReader<OrderProduct> dailyPlusSettlementJpaItemReader() {
        LocalDate now = LocalDate.now();
        LocalDateTime startTime = now.atStartOfDay();
        LocalDateTime endTime = now.plusDays(1).atStartOfDay();

        PurchaseConfirmedJpaQueryProvider queryProvider = new PurchaseConfirmedJpaQueryProvider(startTime, endTime);

        return new JpaPagingItemReaderBuilder<OrderProduct>()
                .name("dailyPlusSettlementJpaItemReader")
                .pageSize(CHUNK_SIZE)
                .queryProvider(queryProvider)
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    @Bean
    public ItemProcessor<OrderProduct, DailySettlementDetail> dailyPlusSettlementItemProcessor() {
        return (orderProduct) -> {
            PositiveDailySettlementCollection collection = new PositiveDailySettlementCollection(orderProduct);
            return collection.getDailySettlementDetail();
        };
    }

    @Bean
    public ItemWriter<DailySettlementDetail> dailyPlusSettlementItemWriter() {
        return (dailySettlementDetails) -> {

            for (DailySettlementDetail dailySettlementDetail : dailySettlementDetails) {

            }
        };
    }
    // === minusSettlementStep ===
}
