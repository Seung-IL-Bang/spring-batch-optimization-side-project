package com.project.batch_service.jobs.daily_settle.steps;

import com.project.batch_service.domain.settlement.DailySettlement;
import com.project.batch_service.domain.settlement.repository.DailySettlementRepository;
import com.project.batch_service.jobs.JobParameterUtils;
import com.project.batch_service.jobs.daily_settle.dto.SettlementAggregate;
import com.project.batch_service.jobs.daily_settle.dto.SettlementAggregationResult;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.project.batch_service.domain.settlement.SettlementStatus.COMPLETED;
import static com.project.batch_service.domain.settlement.SettlementStatus.REFUNDED;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AggregateDailySettlementStepConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final DailySettlementRepository settlementRepository;
    private final DataSource dataSource;
    private final RedisClient redisClient;
    private final RedisAsyncCommands<String, String> redisAsyncCommands;

    @Bean
    @StepScope
    public JdbcCursorItemReader<SettlementAggregate> settlementDetailReader(
            @Value("#{jobParameters['settlementDate']}") String settlementDateStr
    ) {
        LocalDate settlementDate = JobParameterUtils.parseSettlementDate(settlementDateStr);

        // 상세 데이터를 가져오는 쿼리 (집계 없이 원본 데이터만 가져옴)
        String query = """
                SELECT
                    ds.settlement_id,
                    ds.seller_id,
                    ds.settlement_date,
                    dsd.settlement_status,
                    dsd.quantity,
                    dsd.sales_amount,
                    dsd.tax_amount,
                    dsd.promotion_discount_amount,
                    dsd.coupon_discount_amount,
                    dsd.point_used_amount,
                    dsd.shipping_fee,
                    dsd.claim_shipping_fee,
                    dsd.commission_amount,
                    dsd.settlement_amount
                FROM
                    daily_settlement_detail dsd
                JOIN
                    daily_settlement ds ON dsd.daily_settlement_id = ds.settlement_id
                WHERE
                    ds.settlement_date = ?
                """;

        JdbcCursorItemReader<SettlementAggregate> reader = new JdbcCursorItemReader<>();
        reader.setName("settlementDetailJdbcReader");
        reader.setDataSource(dataSource);
        reader.setSql(query);
        reader.setPreparedStatementSetter(ps -> ps.setDate(1, java.sql.Date.valueOf(settlementDate)));
        reader.setRowMapper((rs, rowNum) -> new SettlementAggregate(
                rs.getLong("settlement_id"),
                rs.getLong("seller_id"),
                rs.getDate("settlement_date").toLocalDate(),
                rs.getString("settlement_status"),
                rs.getInt("quantity"),
                rs.getBigDecimal("tax_amount"),
                rs.getBigDecimal("sales_amount"),
                rs.getBigDecimal("promotion_discount_amount"),
                rs.getBigDecimal("coupon_discount_amount"),
                rs.getBigDecimal("point_used_amount"),
                rs.getBigDecimal("shipping_fee"),
                rs.getBigDecimal("claim_shipping_fee"),
                rs.getBigDecimal("commission_amount"),
                rs.getBigDecimal("settlement_amount")
        ));
        return reader;
    }

    @Bean
    public ItemWriter<SettlementAggregate> optimizedRedisAggregateWriter() {
        return items -> {
            // 청크의
            // 파이프라인 시작
            redisAsyncCommands.multi();

            log.info("Processing {} items in a single Redis pipeline transaction", items.size());

            for (SettlementAggregate detail : items) {
                String key = "daily_settlement:" + detail.getSettlementId();

                // 상태별 처리
                if (COMPLETED.name().equals(detail.getSettlementStatus())) {
                    redisAsyncCommands.hincrby(key, "totalOrderCount", 1);
                    redisAsyncCommands.hincrby(key, "totalQuantity", detail.getQuantity());
                } else if (REFUNDED.name().equals(detail.getSettlementStatus())) {
                    redisAsyncCommands.hincrby(key, "totalClaimCount", 1);
                    redisAsyncCommands.hincrby(key, "totalQuantity", -detail.getQuantity());
                }

                // 금액 관련 필드 처리
                String statusMultiplier = REFUNDED.name().equals(detail.getSettlementStatus()) ? "-" : "";

                redisAsyncCommands.hincrbyfloat(key, "totalSalesAmount",
                        Double.parseDouble(statusMultiplier + detail.getSalesAmount().toString()));
                redisAsyncCommands.hincrbyfloat(key, "totalTaxAmount",
                        Double.parseDouble(statusMultiplier + detail.getTaxAmount().toString()));
                redisAsyncCommands.hincrbyfloat(key, "totalPromotionDiscountAmount",
                        Double.parseDouble(statusMultiplier + detail.getPromotionDiscountAmount().toString()));
                redisAsyncCommands.hincrbyfloat(key, "totalCouponDiscountAmount",
                        Double.parseDouble(statusMultiplier + detail.getCouponDiscountAmount().toString()));
                redisAsyncCommands.hincrbyfloat(key, "totalPointUsedAmount",
                        Double.parseDouble(statusMultiplier + detail.getPointUsedAmount().toString()));
                redisAsyncCommands.hincrbyfloat(key, "totalShippingFee",
                        Double.parseDouble(statusMultiplier + detail.getShippingFee().toString()));
                redisAsyncCommands.hincrbyfloat(key, "totalCommissionAmount",
                        Double.parseDouble(statusMultiplier + detail.getCommissionAmount().toString()));
                redisAsyncCommands.hincrbyfloat(key, "totalSettlementAmount",
                        Double.parseDouble(statusMultiplier + detail.getSettlementAmount().toString()));

                // REFUNDED인 경우 클레임 배송비 추가
                if (REFUNDED.name().equals(detail.getSettlementStatus())) {
                    redisAsyncCommands.hincrbyfloat(key, "totalClaimShippingFee",
                            Double.parseDouble(detail.getClaimShippingFee().toString()));
                }

                // seller_id와 settlement_date 저장 (덮어쓰기 - 모든 레코드가 동일한 값을 가짐)
                redisAsyncCommands.hset(key, "sellerId", detail.getSellerId().toString());
                redisAsyncCommands.hset(key, "settlementDate", detail.getSettlementDate().toString());

                // 해당 settlement에 대한 키 목록에 추가
                redisAsyncCommands.sadd("settlement_keys:" + detail.getSettlementDate().toString(), key);
            }

            // 모든 명령을 한 번에 실행
            redisAsyncCommands.exec();
            log.info("Successfully processed batch of {} settlement details to Redis", items.size());
        };
    }

    @Bean
    @StepScope
    public ItemReader<SettlementAggregationResult> redisAggregationResultReader(
            @Value("#{jobParameters['settlementDate']}") String settlementDateStr
    ) {
        return new ItemReader<SettlementAggregationResult>() {

            private boolean processed = false;
            private List<SettlementAggregationResult> results;
            private int currentIndex = 0;

            @Override
            public SettlementAggregationResult read() {
                if (!processed) {
                    results = getAggregationResultsFromRedis(JobParameterUtils.parseSettlementDate(settlementDateStr));
                    processed = true;
                }
                if (currentIndex < results.size()) {
                    return results.get(currentIndex++);
                } else {
                    return null;
                }
            }

            private List<SettlementAggregationResult> getAggregationResultsFromRedis(LocalDate settlementDate) {
                List<SettlementAggregationResult> aggregationResults = new ArrayList<>();

                try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
                    RedisCommands<String, String> sync = connection.sync();

                    // settlement_keys 에서 해당 날짜의 모든 키를 가져옴
                    for (String key : sync.smembers("settlement_keys:" + settlementDate.toString())) {
                        Map<String, String> data = sync.hgetall(key);

                        // Redis 에서 가져온 데이터로 집계 결과 객체 생성
                        SettlementAggregationResult result = new SettlementAggregationResult(
                                Long.parseLong(key.split(":")[1]), // settlement_id
                                Long.parseLong(data.getOrDefault("sellerId", "0")),
                                LocalDate.parse(data.getOrDefault("settlementDate", settlementDate.toString())),
                                Integer.parseInt(data.getOrDefault("totalOrderCount", "0")),
                                Integer.parseInt(data.getOrDefault("totalClaimCount", "0")),
                                Integer.parseInt(data.getOrDefault("totalQuantity", "0")),
                                new BigDecimal(data.getOrDefault("totalSalesAmount", "0")),
                                new BigDecimal(data.getOrDefault("totalTaxAmount", "0")),
                                new BigDecimal(data.getOrDefault("totalPromotionDiscountAmount", "0")),
                                new BigDecimal(data.getOrDefault("totalCouponDiscountAmount", "0")),
                                new BigDecimal(data.getOrDefault("totalPointUsedAmount", "0")),
                                new BigDecimal(data.getOrDefault("totalShippingFee", "0")),
                                new BigDecimal(data.getOrDefault("totalClaimShippingFee", "0")),
                                new BigDecimal(data.getOrDefault("totalCommissionAmount", "0")),
                                new BigDecimal(data.getOrDefault("totalSettlementAmount", "0"))
                        );

                        aggregationResults.add(result);
                    }
                    return aggregationResults;
                }
            }
        };
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

            return settlement;
        };
    }

    @Bean
    public JpaItemWriter<DailySettlement> aggregateDailySettlementWriter() {
        JpaItemWriter<DailySettlement> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }

}
