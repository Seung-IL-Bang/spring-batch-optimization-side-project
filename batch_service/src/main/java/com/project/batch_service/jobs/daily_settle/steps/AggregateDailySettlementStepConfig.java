package com.project.batch_service.jobs.daily_settle.steps;

import com.project.batch_service.domain.settlement.DailySettlement;
import com.project.batch_service.domain.settlement.repository.DailySettlementRepository;
import com.project.batch_service.jobs.JobParameterUtils;
import jakarta.persistence.EntityManagerFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AggregateDailySettlementStepConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final DailySettlementRepository settlementRepository;
    private final DataSource dataSource;

    @Bean
    @StepScope
    public JdbcCursorItemReader<SettlementAggregationResult> aggregationResultReader(
            @Value("#{jobParameters['settlementDate']}") String settlementDateStr,
            @Value("#{jobParameters['chunkSize']}") Integer chunkSize
    ) {
        int CHUNK_SIZE = JobParameterUtils.parseChunkSize(chunkSize);
        LocalDate settlementDate = JobParameterUtils.parseSettlementDate(settlementDateStr);
        // 네이티브 쿼리를 사용한 집계 쿼리
        String nativeQuery = """
                    select
                           ds.settlement_id,
                           ds.seller_id,
                           ds.settlement_date,
                           SUM(IF(dsd.settlement_status = 'COMPLETED', 1, 0)) AS total_order_count,
                           SUM(IF(dsd.settlement_status = 'REFUNDED', 1, 0)) AS total_claim_count,
                           SUM(CASE
                                   WHEN dsd.settlement_status = 'COMPLETED' THEN dsd.quantity
                                   WHEN dsd.settlement_status = 'REFUNDED' THEN -dsd.quantity
                                   ELSE 0
                               END) AS total_quantity,
                           SUM(CASE
                                   WHEN dsd.settlement_status = 'COMPLETED' THEN dsd.sales_amount
                                   WHEN dsd.settlement_status = 'REFUNDED' THEN -dsd.sales_amount
                                   ELSE 0
                               END) AS total_sales_amount,
                           SUM(CASE
                                   WHEN dsd.settlement_status = 'COMPLETED' THEN dsd.tax_amount
                                   WHEN dsd.settlement_status = 'REFUNDED' THEN -dsd.tax_amount
                                   ELSE 0
                               END) AS total_tax_amount,
                           SUM(CASE
                                   WHEN dsd.settlement_status = 'COMPLETED' THEN dsd.promotion_discount_amount
                                   WHEN dsd.settlement_status = 'REFUNDED' THEN -dsd.promotion_discount_amount
                                   ELSE 0
                               END) AS total_promotion_discount_amount,
                           SUM(CASE
                                   WHEN dsd.settlement_status = 'COMPLETED' THEN dsd.coupon_discount_amount
                                   WHEN dsd.settlement_status = 'REFUNDED' THEN -dsd.coupon_discount_amount
                                   ELSE 0
                               END) AS total_coupon_discount_amount,
                           SUM(CASE
                                   WHEN dsd.settlement_status = 'COMPLETED' THEN dsd.point_used_amount
                                   WHEN dsd.settlement_status = 'REFUNDED' THEN -dsd.point_used_amount
                                   ELSE 0
                               END) AS total_point_used_amount,
                           SUM(CASE
                                   WHEN dsd.settlement_status = 'COMPLETED' THEN dsd.shipping_fee
                                   WHEN dsd.settlement_status = 'REFUNDED' THEN -dsd.shipping_fee
                                   ELSE 0
                               END) AS total_shipping_fee,
                           SUM(IF(dsd.settlement_status = 'REFUNDED', dsd.claim_shipping_fee, 0)) AS total_claim_shipping_fee,
                           SUM(CASE
                                   WHEN dsd.settlement_status = 'COMPLETED' THEN dsd.commission_amount
                                   WHEN dsd.settlement_status = 'REFUNDED' THEN -dsd.commission_amount
                                   ELSE 0
                               END) AS total_commission_amount,
                           SUM(CASE
                                   WHEN dsd.settlement_status = 'COMPLETED' THEN dsd.settlement_amount
                                   WHEN dsd.settlement_status = 'REFUNDED' THEN -dsd.settlement_amount
                                   ELSE 0
                               END) AS total_settlement_amount
                    from daily_settlement_detail dsd
                             join daily_settlement ds on dsd.daily_settlement_id = ds.settlement_id
                    where ds.settlement_date = ?
                    group by ds.settlement_id, ds.seller_id, ds.settlement_date
                """;

        // JdbcCursorItemReader -> 파라미터 ? 설정 주의 namedParameterJdbcTemplate 사용 불가

        JdbcCursorItemReader<SettlementAggregationResult> reader = new JdbcCursorItemReader<>();
        reader.setName("aggregationResultJdbcReader");
        reader.setDataSource(dataSource);
        reader.setSql(nativeQuery);
        reader.setPreparedStatementSetter(ps -> ps.setDate(1, java.sql.Date.valueOf(settlementDate)));
        reader.setFetchSize(CHUNK_SIZE);
        reader.setRowMapper((rs, rowNum) -> new SettlementAggregationResult(
                rs.getLong("settlement_id"),
                rs.getLong("seller_id"),
                rs.getDate("settlement_date").toLocalDate(),
                rs.getInt("total_order_count"),
                rs.getInt("total_claim_count"),
                rs.getInt("total_quantity"),
                rs.getBigDecimal("total_sales_amount"),
                rs.getBigDecimal("total_tax_amount"),
                rs.getBigDecimal("total_promotion_discount_amount"),
                rs.getBigDecimal("total_coupon_discount_amount"),
                rs.getBigDecimal("total_point_used_amount"),
                rs.getBigDecimal("total_shipping_fee"),
                rs.getBigDecimal("total_claim_shipping_fee"),
                rs.getBigDecimal("total_commission_amount"),
                rs.getBigDecimal("total_settlement_amount")
        ));

        return reader;
    }

    @Bean
    public ItemProcessor<SettlementAggregationResult, DailySettlement> aggregateDailySettlementProcessor() {
        return result -> {
            // 집계 결과를 DailySettlement 엔티티로 변환
            DailySettlement settlement = settlementRepository
                    .findById(result.getSettlementId())
                    .orElseThrow(() -> new IllegalArgumentException("DailySettlement is not found"));

            // 집계된 값 설정
            settlement.setTotalOrderCount(result.getTotalOrderCount());
            settlement.setTotalClaimCount(result.getTotalClaimCount());
            settlement.setTotalQuantity(result.getTotalQuantity());
            settlement.setSalesAmount(result.getTotalSalesAmount());
            settlement.setTaxAmount(result.getTotalTaxAmount());
            settlement.setPromotionDiscountAmount(result.getTotalPromotionDiscountAmount());
            settlement.setCouponDiscountAmount(result.getTotalCouponDiscountAmount());
            settlement.setPointUsedAmount(result.getTotalPointUsedAmount());
            settlement.setShippingFee(result.getTotalShippingFee());
            settlement.setClaimShippingFee(result.getTotalClaimShippingFee());
            settlement.setCommissionAmount(result.getTotalCommissionAmount());
            settlement.setTotalSettlementAmount(result.getTotalSettlementAmount());

            // todo created_at, updated_at 설정

            return settlement;
        };
    }

    @Bean
    public JpaItemWriter<DailySettlement> aggregateDailySettlementWriter() {
        JpaItemWriter<DailySettlement> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }

    // 집계 결과를 담을 DTO 클래스
    @Getter
    @AllArgsConstructor
    public static class SettlementAggregationResult {
        private Long settlementId;
        private Long sellerId;
        private LocalDate settlementDate;
        private Integer totalOrderCount;
        private Integer totalClaimCount;
        private Integer totalQuantity;
        private BigDecimal totalSalesAmount;
        private BigDecimal totalTaxAmount;
        private BigDecimal totalPromotionDiscountAmount;
        private BigDecimal totalCouponDiscountAmount;
        private BigDecimal totalPointUsedAmount;
        private BigDecimal totalShippingFee;
        private BigDecimal totalClaimShippingFee;
        private BigDecimal totalCommissionAmount;
        private BigDecimal totalSettlementAmount;
    }

}
