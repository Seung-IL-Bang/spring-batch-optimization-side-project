package com.project.batch_service.job.daily_settle;

import com.project.batch_service.domain.orders.OrderProduct;
import com.project.batch_service.domain.orders.repository.OrderProductRepository;
import com.project.batch_service.domain.settlement.DailySettlement;
import com.project.batch_service.domain.settlement.DailySettlementDetail;
import com.project.batch_service.domain.settlement.repository.DailySettlementDetailRepository;
import com.project.batch_service.domain.settlement.repository.DailySettlementRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
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
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DailySettlementJobConfig {

    private static final int CHUNK_SIZE = 1000;
    public static final String DAILY_SETTLEMENT_JOB = "dailySettlementJob";
    public static final String PURCHASE_CONFIRMED_STEP = DAILY_SETTLEMENT_JOB + "_purchaseConfirmedStep";
    public static final String PLUS_SETTLEMENT_STEP = DAILY_SETTLEMENT_JOB + "_plusSettlementStep";
    public static final String MINUS_SETTLEMENT_STEP = DAILY_SETTLEMENT_JOB + "_minusSettlementStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final OrderProductRepository orderProductRepository;
    private final DailySettlementRepository dailySettlementRepository;
    private final DailySettlementDetailRepository dailySettlementDetailRepository;

    private final CreateDailySettlementStepConfig createDailySettlementStepConfig;
    private final AggregateDailySettlementStepConfig aggregateDailySettlementStepConfig;

    /**
     * TODO
     * 0. DailySettlement (sellerId, settlementDate) UNIQUE
     * 1. 마이너스 정산 (ok)
     * 2. 일일 정산 집계
     * 3. csv 파일 데이터 정합성 맞춰서 테스트
     * 4. 정산 검증
     */


    @Bean
    public Job dailySettlementJob() {
        return new JobBuilder(DAILY_SETTLEMENT_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(purchaseConfirmedStep())
                .next(createDailySettlementStep())
                .next(plusSettlementDetailStep())
                .next(minusSettlementStep())
                .next(aggregateDailySettlementStep())
                .build();
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
    public Step createDailySettlementStep() {
        return new StepBuilder("createDailySettlementStep", jobRepository)
                .<SellerDto, DailySettlement>chunk(CHUNK_SIZE, transactionManager)
                .reader(createDailySettlementStepConfig.sellerReader(null))
                .processor(createDailySettlementStepConfig.dailySettlementProcessor(null))
                .writer(createDailySettlementStepConfig.dailySettlementWriter())
                .build();
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
    public Step plusSettlementDetailStep() {
        return new StepBuilder(PLUS_SETTLEMENT_STEP, jobRepository)
                .<OrderProduct, DailySettlementDetail>chunk(CHUNK_SIZE, transactionManager)
                .reader(dailyPlusSettlementJpaItemReader())
                .processor(dailyPlusSettlementItemProcessor())
                .writer(dailyPlusSettlementItemWriter())
                .build();
    }

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
            PositiveDailySettlementCollection collection = new PositiveDailySettlementCollection(orderProduct, dailySettlementRepository);
            return collection.getDailySettlementDetail();
        };
    }

    @Bean
    public ItemWriter<DailySettlementDetail> dailyPlusSettlementItemWriter() {
        return (dailySettlementDetails) -> {
            for (DailySettlementDetail dailySettlementDetail : dailySettlementDetails) {
                dailySettlementDetailRepository.save(dailySettlementDetail);
            }
        };
    }

    // === minusSettlementStep ===

    @Bean
    public Step minusSettlementStep() {
        return new StepBuilder(MINUS_SETTLEMENT_STEP, jobRepository)
                .<ClaimRefundDto, DailySettlementDetail>chunk(CHUNK_SIZE, transactionManager)
                .reader(dailyMinusSettlementJpaItemReader())
                .processor(dailyMinusSettlementItemProcessor())
                .writer(dailyMinusSettlementItemWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<ClaimRefundDto> dailyMinusSettlementJpaItemReader() {
        LocalDate now = LocalDate.now();
        LocalDateTime startTime = now.atStartOfDay();
        LocalDateTime endTime = now.plusDays(1).atStartOfDay();
        // 클레임 완료된 건들 정산

        ClaimCompletedJpaQueryProvider queryProvider = new ClaimCompletedJpaQueryProvider(startTime, endTime);

        return new JpaPagingItemReaderBuilder<ClaimRefundDto>()
                .name("dailyMinusSettlementJpaItemReader")
                .pageSize(CHUNK_SIZE)
                .queryProvider(queryProvider)
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    @Bean
    public ItemProcessor<ClaimRefundDto, DailySettlementDetail> dailyMinusSettlementItemProcessor() {
        return (claimRefundDto) -> {
            NegativeDailySettlementCollection collection = new NegativeDailySettlementCollection(claimRefundDto, dailySettlementRepository);
            return collection.getDailySettlementDetail();
        };
    }

    @Bean
    public ItemWriter<DailySettlementDetail> dailyMinusSettlementItemWriter() {
        return (dailySettlementDetails) -> {
            ;
            for (DailySettlementDetail dailySettlementDetail : dailySettlementDetails) {
                dailySettlementDetailRepository.save(dailySettlementDetail);
            }
        };
    }

    // === aggregateDailySettlementStep ===
    @Bean
    public Step aggregateDailySettlementStep() {
        return new StepBuilder("aggregateDailySettlementStep", jobRepository)
                .<AggregateDailySettlementStepConfig.SettlementAggregationResult, DailySettlement>chunk(CHUNK_SIZE, transactionManager)
                .reader(aggregateDailySettlementStepConfig.aggregationResultReader(null))
                .processor(aggregateDailySettlementStepConfig.aggregateDailySettlementProcessor())
                .writer(aggregateDailySettlementStepConfig.aggregateDailySettlementWriter())
                .build();
    }
}
