package com.project.batch_service.jobs.daily_settle;

import com.project.batch_service.domain.orders.OrderProduct;
import com.project.batch_service.domain.settlement.DailySettlement;
import com.project.batch_service.domain.settlement.DailySettlementDetail;
import com.project.batch_service.jobs.JobParameterUtils;
import com.project.batch_service.jobs.daily_settle.dto.ClaimRefundDto;
import com.project.batch_service.jobs.daily_settle.dto.SellerDto;
import com.project.batch_service.jobs.daily_settle.listener.StepPerformanceListener;
import com.project.batch_service.jobs.daily_settle.steps.*;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import static com.project.batch_service.constants.BatchNamingConstants.Job.DAILY_SETTLE_JOB;
import static com.project.batch_service.constants.BatchNamingConstants.Step.*;

@EnableTask
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

    private final StepPerformanceListener stepPerformanceListener;

    @Bean
    public Job dailySettlementJob() throws Exception {
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
    public Step purchaseConfirmedStep(@Value("#{jobParameters['chunkSize']}") Integer chunkSize) throws Exception {
        int CHUNK_SIZE = JobParameterUtils.parseChunkSize(chunkSize);
        return new StepBuilder(PURCHASE_CONFIRMED_STEP, jobRepository)
                .<OrderProduct, OrderProduct>chunk(CHUNK_SIZE, transactionManager)
                .reader(purchaseConfirmStepConfig.deliveryCompletedJpaItemReader(null, CHUNK_SIZE))
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
                .processor(plusSettlementDetailStepConfig.dailyPlusSettlementItemProcessor(null))
                .writer(plusSettlementDetailStepConfig.dailyPlusSettlementItemWriter())
                .listener((StepExecutionListener) stepPerformanceListener)
                .listener((ItemReadListener<OrderProduct>) stepPerformanceListener)
                .listener((ItemProcessListener<OrderProduct, DailySettlementDetail>) stepPerformanceListener)
                .listener((ItemWriteListener<DailySettlementDetail>) stepPerformanceListener)
                .build();
    }

    @Bean
    @JobScope
    public Step minusSettlementDetailStep(@Value("#{jobParameters['chunkSize']}") Integer chunkSize) {
        int CHUNK_SIZE = JobParameterUtils.parseChunkSize(chunkSize);
        return new StepBuilder(MINUS_SETTLEMENT_STEP, jobRepository)
                .<ClaimRefundDto, DailySettlementDetail>chunk(CHUNK_SIZE, transactionManager)
                .reader(minusSettlementDetailStepConfig.dailyMinusSettlementJpaItemReader(null, CHUNK_SIZE))
                .processor(minusSettlementDetailStepConfig.dailyMinusSettlementItemProcessor(null))
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
}
