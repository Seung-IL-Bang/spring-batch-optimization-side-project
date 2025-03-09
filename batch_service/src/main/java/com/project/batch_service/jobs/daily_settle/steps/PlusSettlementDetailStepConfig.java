package com.project.batch_service.jobs.daily_settle.steps;

import com.project.batch_service.domain.orders.OrderProduct;
import com.project.batch_service.domain.settlement.DailySettlementDetail;
import com.project.batch_service.domain.settlement.repository.DailySettlementDetailRepository;
import com.project.batch_service.domain.settlement.repository.DailySettlementRepository;
import com.project.batch_service.jobs.daily_settle.utils.JobParameterUtils;
import com.project.batch_service.jobs.daily_settle.utils.PositiveDailySettlementCollection;
import com.project.batch_service.jobs.daily_settle.steps.query.PurchaseConfirmedJpaQueryProvider;
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
public class PlusSettlementDetailStepConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final DailySettlementRepository dailySettlementRepository;
    private final DailySettlementDetailRepository dailySettlementDetailRepository;

    @Bean
    @StepScope
    public JpaPagingItemReader<OrderProduct> dailyPlusSettlementJpaItemReader(
            @Value("#{jobParameters['settlementDate']}") String settlementDateStr,
            @Value("#{jobParameters['chunkSize']}") Integer chunkSize
    ) {
        int CHUNK_SIZE = JobParameterUtils.parseChunkSize(chunkSize);
        LocalDate date = JobParameterUtils.parseSettlementDate(settlementDateStr);
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.plusDays(1).atStartOfDay();

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

}
