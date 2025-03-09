package com.project.batch_service.jobs.daily_settle;

import com.project.batch_service.domain.orders.OrderProduct;
import com.project.batch_service.domain.settlement.DailySettlement;
import com.project.batch_service.domain.settlement.DailySettlementDetail;
import com.project.batch_service.jobs.daily_settle.dto.ClaimRefundDto;
import com.project.batch_service.jobs.daily_settle.dto.SellerDto;
import com.project.batch_service.jobs.daily_settle.steps.*;
import com.project.batch_service.jobs.daily_settle.utils.JobParameterUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import static com.project.batch_service.constants.BatchNamingConstants.Job.DAILY_SETTLE_JOB;
import static com.project.batch_service.constants.BatchNamingConstants.Step.*;

@Configuration
@RequiredArgsConstructor
public class DailySettlementJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final CreateDailySettlementStepConfig createDailySettlementStepConfig;
    private final PurchaseConfirmStepConfig purchaseConfirmStepConfig;
    private final PlusSettlementDetailStepConfig plusSettlementDetailStepConfig;
    private final MinusSettlementDetailStepConfig minusSettlementDetailStepConfig;
    private final AggregateDailySettlementStepConfig aggregateDailySettlementStepConfig;

    @Bean
    public Job dailySettlementJob() {
        return new JobBuilder(DAILY_SETTLE_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(purchaseConfirmedStep(null))
                .next(createDailySettlementStep(null))
                .next(plusSettlementDetailStep(null))
                .next(minusSettlementDetailStep(null))
                .next(aggregateSettlementDetailStep(null))
                .build();
    }

    @Bean
    @JobScope
    public Step purchaseConfirmedStep(@Value("#{jobParameters['chunkSize']}") Integer chunkSize) {
        int CHUNK_SIZE = JobParameterUtils.parseChunkSize(chunkSize);
        return new StepBuilder(PURCHASE_CONFIRMED_STEP, jobRepository)
                .<OrderProduct, OrderProduct>chunk(CHUNK_SIZE, transactionManager)
                .reader(purchaseConfirmStepConfig.deliveryCompletedJpaItemReader(null, CHUNK_SIZE))
                .processor(purchaseConfirmStepConfig.purchaseConfirmedItemProcessor())
                .writer(purchaseConfirmStepConfig.purchaseConfirmedItemWriter())
                .build();
    }

    @Bean
    @JobScope
    public Step createDailySettlementStep(@Value("#{jobParameters['chunkSize']}") Integer chunkSize) {
        int CHUNK_SIZE = JobParameterUtils.parseChunkSize(chunkSize);
        return new StepBuilder(CREATE_DAILY_SETTLEMENT_STEP, jobRepository)
                .<SellerDto, DailySettlement>chunk(CHUNK_SIZE, transactionManager)
                .reader(createDailySettlementStepConfig.sellerReader(null, CHUNK_SIZE))
                .processor(createDailySettlementStepConfig.dailySettlementProcessor(null))
                .writer(createDailySettlementStepConfig.dailySettlementWriter())
                .build();
    }

    @Bean
    @JobScope
    public Step plusSettlementDetailStep(@Value("#{jobParameters['chunkSize']}") Integer chunkSize) {
        int CHUNK_SIZE = JobParameterUtils.parseChunkSize(chunkSize);
        return new StepBuilder(PLUS_SETTLEMENT_STEP, jobRepository)
                .<OrderProduct, DailySettlementDetail>chunk(CHUNK_SIZE, transactionManager)
                .reader(plusSettlementDetailStepConfig.dailyPlusSettlementJpaItemReader(null, CHUNK_SIZE))
                .processor(plusSettlementDetailStepConfig.dailyPlusSettlementItemProcessor())
                .writer(plusSettlementDetailStepConfig.dailyPlusSettlementItemWriter())
                .build();
    }

    @Bean
    @JobScope
    public Step minusSettlementDetailStep(@Value("#{jobParameters['chunkSize']}") Integer chunkSize) {
        int CHUNK_SIZE = JobParameterUtils.parseChunkSize(chunkSize);
        return new StepBuilder(MINUS_SETTLEMENT_STEP, jobRepository)
                .<ClaimRefundDto, DailySettlementDetail>chunk(CHUNK_SIZE, transactionManager)
                .reader(minusSettlementDetailStepConfig.dailyMinusSettlementJpaItemReader(null, CHUNK_SIZE))
                .processor(minusSettlementDetailStepConfig.dailyMinusSettlementItemProcessor())
                .writer(minusSettlementDetailStepConfig.dailyMinusSettlementItemWriter())
                .build();
    }

    @Bean
    @JobScope
    public Step aggregateSettlementDetailStep(@Value("#{jobParameters['chunkSize']}") Integer chunkSize) {
        int CHUNK_SIZE = JobParameterUtils.parseChunkSize(chunkSize);
        return new StepBuilder(AGGREGATE_SETTLEMENT_STEP, jobRepository)
                .<AggregateDailySettlementStepConfig.SettlementAggregationResult, DailySettlement>chunk(CHUNK_SIZE, transactionManager)
                .reader(aggregateDailySettlementStepConfig.aggregationResultReader(null, CHUNK_SIZE))
                .processor(aggregateDailySettlementStepConfig.aggregateDailySettlementProcessor())
                .writer(aggregateDailySettlementStepConfig.aggregateDailySettlementWriter())
                .build();
    }

    /**
     * TODO
     * 0. DailySettlement (sellerId, settlementDate) UNIQUE
     * 1. 마이너스 정산 (ok)
     * 2. 일일 정산 집계 (ok)
     * 3. csv 파일 데이터 정합성 맞춰서 테스트
     * 4. 정산 검증
     */
}
