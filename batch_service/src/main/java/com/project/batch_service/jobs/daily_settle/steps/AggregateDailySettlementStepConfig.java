package com.project.batch_service.jobs.daily_settle.steps;

import com.project.batch_service.domain.settlement.DailySettlement;
import com.project.batch_service.domain.settlement.repository.DailySettlementRepository;
import com.project.batch_service.jobs.JobParameterUtils;
import com.project.batch_service.jobs.daily_settle.dto.SettlementAggregate;
import com.project.batch_service.jobs.daily_settle.dto.SettlementAggregationResult;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.project.batch_service.domain.settlement.SettlementStatus.COMPLETED;
import static com.project.batch_service.domain.settlement.SettlementStatus.REFUNDED;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AggregateDailySettlementStepConfig {

    private final DataSource dataSource;
    private final RedisClient redisClient;

    // 집계를 위한 LUA 스크립트
    private static final String AGGREGATE_SCRIPT = """
            local key = KEYS[1]
            local settlement_key = KEYS[2]
            local status = ARGV[1]
            local seller_id = ARGV[2]
            local settlement_date = ARGV[3]
            local quantity = tonumber(ARGV[4])
            local tax_amount = tonumber(ARGV[5])
            local sales_amount = tonumber(ARGV[6])
            local promotion_discount_amount = tonumber(ARGV[7])
            local coupon_discount_amount = tonumber(ARGV[8])
            local point_used_amount = tonumber(ARGV[9])
            local shipping_fee = tonumber(ARGV[10])
            local claim_shipping_fee = tonumber(ARGV[11])
            local commission_amount = tonumber(ARGV[12])
            local settlement_amount = tonumber(ARGV[13])
            
            if status == 'COMPLETED' then
              redis.call('hincrby', key, 'totalOrderCount', 1)
              redis.call('hincrby', key, 'totalQuantity', quantity)
            else
              redis.call('hincrby', key, 'totalClaimCount', 1)
              redis.call('hincrby', key, 'totalQuantity', -quantity)
            end
            
            local multiplier = status == 'REFUNDED' and -1 or 1
            redis.call('hincrbyfloat', key, 'totalSalesAmount', multiplier * sales_amount)
            redis.call('hincrbyfloat', key, 'totalTaxAmount', multiplier * tax_amount)
            redis.call('hincrbyfloat', key, 'totalPromotionDiscountAmount', multiplier * promotion_discount_amount)
            redis.call('hincrbyfloat', key, 'totalCouponDiscountAmount', multiplier * coupon_discount_amount)
            redis.call('hincrbyfloat', key, 'totalPointUsedAmount', multiplier * point_used_amount)
            redis.call('hincrbyfloat', key, 'totalShippingFee', multiplier * shipping_fee)
            redis.call('hincrbyfloat', key, 'totalCommissionAmount', multiplier * commission_amount)
            redis.call('hincrbyfloat', key, 'totalSettlementAmount', multiplier * settlement_amount)
            
            if status == 'REFUNDED' then
              redis.call('hincrbyfloat', key, 'totalClaimShippingFee', claim_shipping_fee)
            end
            
            redis.call('hset', key, 'sellerId', seller_id)
            redis.call('hset', key, 'settlementDate', settlement_date)
            redis.call('sadd', settlement_key, key)
            
            return 1
            """;

    @Bean
    @StepScope
    public JdbcCursorItemReader<SettlementAggregate> settlementDetailReader(
            @Value("#{jobParameters['settlementDate']}") String settlementDateStr,
            @Value("#{jobParameters['chunkSize']}") Integer chunkSize
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
    public ItemWriter<SettlementAggregate> luaScriptDirectWriter() {
        return items -> {
            log.info("Processing {} items with Redis Lua Script direct execution", items.size());

            // LUA 스크립트를 활용한 처리
            try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
                RedisAsyncCommands<String, String> async = connection.async();
                async.setAutoFlushCommands(false);

                List<RedisFuture<?>> futures = new ArrayList<>();

                for (SettlementAggregate detail : items) {
                    String key = "daily_settlement:" + detail.getSettlementId();
                    String settlementKeysKey = "settlement_keys:" + detail.getSettlementDate().toString();

                    // LUA 스크립트 실행 - 모든 연산을 레디스 서버에서 처리
                    futures.add(async.eval(
                            AGGREGATE_SCRIPT,
                            ScriptOutputType.INTEGER,
                            new String[]{key, settlementKeysKey},
                            detail.getSettlementStatus(),
                            detail.getSellerId().toString(),
                            detail.getSettlementDate().toString(),
                            String.valueOf(detail.getQuantity()),
                            detail.getTaxAmount().toString(),
                            detail.getSalesAmount().toString(),
                            detail.getPromotionDiscountAmount().toString(),
                            detail.getCouponDiscountAmount().toString(),
                            detail.getPointUsedAmount().toString(),
                            detail.getShippingFee().toString(),
                            detail.getClaimShippingFee().toString(),
                            detail.getCommissionAmount().toString(),
                            detail.getSettlementAmount().toString()
                    ));
                }

                // 모든 명령어 한번에 전송
                async.flushCommands();

                // 모든 작업 완료 대기
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Error processing settlements with Lua script: {}", e.getMessage(), e);
                throw new RuntimeException("Error executing Redis Lua script", e);
            }

            log.info("Successfully processed batch of {} settlement details to Redis using Lua script", items.size());
        };
    }

    @Bean
    @StepScope
    public ItemReader<SettlementAggregationResult> optimizedRedisAggregationResultReader(
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
                String settlementKeysKey = "settlement_keys:" + settlementDate.toString();

                try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
                    RedisCommands<String, String> sync = connection.sync();

                    // 1. 키 목록 조회
                    Set<String> keys = sync.smembers(settlementKeysKey);

                    if (keys.isEmpty()) {
                        return aggregationResults;
                    }

                    // 2. 파이프라인 사용하여 한 번에 여러 키 조회
                    RedisAsyncCommands<String, String> async = connection.async();
                    async.setAutoFlushCommands(false);  // 자동 플러시 비활성화

                    Map<String, CompletableFuture<Map<String, String>>> resultFutures = new HashMap<>();

                    // 모든 키에 대한 HGETALL 명령 준비
                    for (String key : keys) {
                        resultFutures.put(key, async.hgetall(key).toCompletableFuture());
                    }

                    // 모든 명령어 한 번에 전송
                    async.flushCommands();

                    // 3. 결과 처리
                    for (String key : keys) {
                        try {
                            Map<String, String> data = resultFutures.get(key).get(10, TimeUnit.SECONDS);

                            if (data != null && !data.isEmpty()) {
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
                        } catch (Exception e) {
                            log.warn("Error getting data for key {}: {}", key, e.getMessage());
                            // 개별 키 처리 실패 시에도 계속 진행
                        }
                    }

                    return aggregationResults;
                } catch (Exception e) {
                    log.error("Error reading aggregation results from Redis: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to read aggregation results", e);
                }
            }
        };
    }

    @Bean
    public ItemWriter<SettlementAggregationResult> optimizedAggregateDailySettlementJdbcWriter() {
        return new ItemWriter<SettlementAggregationResult>() {
            @Autowired
            private JdbcTemplate jdbcTemplate;

            @Override
            public void write(Chunk<? extends SettlementAggregationResult> items) throws Exception {
                if (items.isEmpty()) {
                    return;
                }

                log.info("Processing {} settlement aggregation results with JDBC batch update", items.size());

                String updateSql = """
                            UPDATE daily_settlement SET
                                total_order_count = ?,
                                total_claim_count = ?,
                                total_quantity = ?,
                                sales_amount = ?,
                                tax_amount = ?,
                                promotion_discount_amount = ?,
                                coupon_discount_amount = ?,
                                point_used_amount = ?,
                                shipping_fee = ?,
                                claim_shipping_fee = ?,
                                commission_amount = ?,
                                total_settlement_amount = ?,
                                updated_at = ?
                            WHERE settlement_id = ?
                        """;

                jdbcTemplate.batchUpdate(updateSql, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        SettlementAggregationResult result = items.getItems().get(i);
                        LocalDateTime now = LocalDateTime.now();
                        int idx = 1;

                        // SET clause parameters
                        ps.setInt(idx++, result.getTotalOrderCount());
                        ps.setInt(idx++, result.getTotalClaimCount());
                        ps.setInt(idx++, result.getTotalQuantity());
                        ps.setBigDecimal(idx++, result.getTotalSalesAmount());
                        ps.setBigDecimal(idx++, result.getTotalTaxAmount());
                        ps.setBigDecimal(idx++, result.getTotalPromotionDiscountAmount());
                        ps.setBigDecimal(idx++, result.getTotalCouponDiscountAmount());
                        ps.setBigDecimal(idx++, result.getTotalPointUsedAmount());
                        ps.setBigDecimal(idx++, result.getTotalShippingFee());
                        ps.setBigDecimal(idx++, result.getTotalClaimShippingFee());
                        ps.setBigDecimal(idx++, result.getTotalCommissionAmount());
                        ps.setBigDecimal(idx++, result.getTotalSettlementAmount());
                        ps.setTimestamp(idx++, Timestamp.valueOf(now));

                        // WHERE clause parameter
                        ps.setLong(idx++, result.getSettlementId());
                    }

                    @Override
                    public int getBatchSize() {
                        return items.size();
                    }
                });

                log.info("Successfully updated {} daily settlements with batch update", items.size());
            }
        };
    }
}
