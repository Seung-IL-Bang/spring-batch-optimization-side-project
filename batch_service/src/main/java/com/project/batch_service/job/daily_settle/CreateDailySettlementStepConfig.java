package com.project.batch_service.job.daily_settle;

import com.project.batch_service.domain.settlement.DailySettlement;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.orm.AbstractJpaQueryProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Configuration
@RequiredArgsConstructor
public class CreateDailySettlementStepConfig {

    private final EntityManagerFactory entityManagerFactory;

    @Bean
    @StepScope
    public JpaPagingItemReader<SellerDto> sellerReader(
            @Value("#{jobParameters['settlementDate']}") String settlementDateStr) {
        // 정산일 파라미터 파싱 (기본값: 오늘)
        LocalDate settlementDate = settlementDateStr != null ?
                LocalDate.parse(settlementDateStr, DateTimeFormatter.ofPattern("yyyyMMdd")) :
                LocalDate.now();

        LocalDateTime startOfDay = settlementDate.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        // JPQL을 사용하여 정산 대상 셀러 조회
        // 구매 확정이면서 DELIVERED인 주문이 있는 셀러만 조회
        return new JpaPagingItemReaderBuilder<SellerDto>()
                .name("sellerReader")
                .entityManagerFactory(entityManagerFactory)
                .queryProvider(new AbstractJpaQueryProvider() {
                    @Override
                    public Query createQuery() {
                        return this.getEntityManager().createQuery(
                                        "SELECT new com.project.batch_service.job.daily_settle.SellerDto(p.seller.sellerId) " +
                                                "FROM OrderProduct op " +
                                                "JOIN Products p ON p.productId = op.product.productId " +
                                                "WHERE op.purchaseConfirmedAt is NOT NULL " +
                                                "AND op.purchaseConfirmedAt BETWEEN :startDate AND :endDate " +
                                                "AND op.deliveryCompletedAt is NOT NULL " +
                                                "GROUP BY p.seller.sellerId", SellerDto.class)
                                .setParameter("startDate", startOfDay)
                                .setParameter("endDate", endOfDay);
                    }

                    @Override
                    public void afterPropertiesSet() throws Exception {

                    }
                })
                .pageSize(100)
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<SellerDto, DailySettlement> dailySettlementProcessor(
            @Value("#{jobParameters['settlementDate']}") String settlementDateStr
    ) {
        return sellerDto -> {

            LocalDate settlementDate = settlementDateStr != null ?
                    LocalDate.parse(settlementDateStr, DateTimeFormatter.ofPattern("yyyyMMdd")) :
                    LocalDate.now();

            return DailySettlement
                    .builder()
                    .sellerId(sellerDto.getSellerId())
                    .settlementDate(settlementDate)
                    .totalOrderCount(0)
                    .totalClaimCount(0)
                    .totalProductCount(0)
                    .totalQuantity(0)
                    .taxAmount(BigDecimal.ZERO)
                    .salesAmount(BigDecimal.ZERO)
                    .promotionDiscountAmount(BigDecimal.ZERO)
                    .couponDiscountAmount(BigDecimal.ZERO)
                    .pointUsedAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .claimShippingFee(BigDecimal.ZERO)
                    .commissionAmount(BigDecimal.ZERO)
                    .build();
        };
    }

    @Bean
    public JpaItemWriter<DailySettlement> dailySettlementWriter() {
        JpaItemWriter<DailySettlement> jpaItemWriter = new JpaItemWriter<>();
        jpaItemWriter.setEntityManagerFactory(entityManagerFactory);
        return jpaItemWriter;
    }


}
