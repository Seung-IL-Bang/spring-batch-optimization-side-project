package com.project.batch_service.jobs.daily_settle.steps;

import com.project.batch_service.domain.settlement.DailySettlementDetail;
import com.project.batch_service.domain.settlement.repository.DailySettlementDetailRepository;
import com.project.batch_service.domain.settlement.repository.DailySettlementRepository;
import com.project.batch_service.jobs.daily_settle.steps.query.ClaimCompletedJpaQueryProvider;
import com.project.batch_service.jobs.daily_settle.dto.ClaimRefundDto;
import com.project.batch_service.jobs.daily_settle.utils.JobParameterUtils;
import com.project.batch_service.jobs.daily_settle.utils.NegativeDailySettlementCollection;
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
public class MinusSettlementDetailStepConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final DailySettlementRepository dailySettlementRepository;
    private final DailySettlementDetailRepository dailySettlementDetailRepository;

    @Bean
    @StepScope
    public JpaPagingItemReader<ClaimRefundDto> dailyMinusSettlementJpaItemReader(
            @Value("#{jobParameters['settlementDate']}") String settlementDateStr,
            @Value("#{jobParameters['chunkSize']}") Integer chunkSize
    ) {
        int CHUNK_SIZE = JobParameterUtils.parseChunkSize(chunkSize);
        LocalDate date = JobParameterUtils.parseSettlementDate(settlementDateStr);
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.plusDays(1).atStartOfDay();

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

}
