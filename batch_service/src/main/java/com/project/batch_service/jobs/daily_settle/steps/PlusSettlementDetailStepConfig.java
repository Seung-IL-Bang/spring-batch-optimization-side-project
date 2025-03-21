package com.project.batch_service.jobs.daily_settle.steps;

import com.project.batch_service.domain.products.TaxType;
import com.project.batch_service.domain.settlement.DailySettlementDetail;
import com.project.batch_service.domain.settlement.repository.DailySettlementRepository;
import com.project.batch_service.jobs.JobParameterUtils;
import com.project.batch_service.jobs.daily_settle.dto.OrderProductDTO;
import com.project.batch_service.jobs.daily_settle.utils.PositiveDailySettlementCollection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PlusSettlementDetailStepConfig {

    private final DataSource dataSource;
    private final DailySettlementRepository dailySettlementRepository;

    @Bean
    @StepScope
    public JdbcPagingItemReader<OrderProductDTO> dailyPlusSettlementJpaItemReader(
            @Value("#{jobParameters['settlementDate']}") String settlementDateStr,
            @Value("#{jobParameters['chunkSize']}") Integer chunkSize
    ) throws Exception {
        int CHUNK_SIZE = JobParameterUtils.parseChunkSize(chunkSize);
        LocalDate date = JobParameterUtils.parseSettlementDate(settlementDateStr);
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.plusDays(1).atStartOfDay();

        SqlPagingQueryProviderFactoryBean queryProviderFactory = new SqlPagingQueryProviderFactoryBean();
        queryProviderFactory.setDataSource(dataSource);

        // 필요한 모든 필드를 한 번에 조회하는 SELECT 절
        queryProviderFactory.setSelectClause("""
                SELECT
                    op.order_product_id,
                    o.order_id,
                    p.product_id,
                    s.seller_id,
                    ops.quantity,
                    ops.sell_price,
                    ops.supply_price,
                    s.commission_rate,
                    ops.tax_type,
                    ops.promotion_discount_amount,
                    ops.coupon_discount_amount,
                    ops.point_used_amount,
                    ops.default_delivery_amount,
                    ds.settlement_id
                """);

        // 모든 필요한 테이블을 JOIN
        queryProviderFactory.setFromClause("""
                FROM order_product op
                JOIN order_product_snapshot ops ON op.order_product_snapshot_id = ops.order_product_snapshot_id
                JOIN orders o ON op.order_id = o.order_id
                JOIN products p ON op.product_id = p.product_id
                JOIN seller s ON p.seller_id = s.seller_id
                LEFT JOIN daily_settlement ds ON s.seller_id = ds.seller_id AND ds.settlement_date = :settlementDate
                """);

        // 필터링 조건
        queryProviderFactory.setWhereClause("""
                WHERE op.purchase_confirmed_at BETWEEN :startTime AND :endTime
                AND op.delivery_status = 'DELIVERED'
                """);

        // 정렬 기준 (성능을 위해 인덱스가 있는 컬럼 사용)
        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("op.order_product_id", Order.ASCENDING);
        queryProviderFactory.setSortKeys(sortKeys);

        // 쿼리 파라미터 설정
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("startTime", Timestamp.valueOf(startTime));
        parameterValues.put("endTime", Timestamp.valueOf(endTime));
        parameterValues.put("settlementDate", date);

        PagingQueryProvider queryProvider = queryProviderFactory.getObject();

        // RowMapper 구현 (ResultSet을 DTO로 변환)
        RowMapper<OrderProductDTO> rowMapper = (rs, rowNum) -> {
            OrderProductDTO dto = new OrderProductDTO();
            dto.setOrderProductId(rs.getLong("order_product_id"));
            dto.setOrderId(rs.getLong("order_id"));
            dto.setProductId(rs.getLong("product_id"));
            dto.setSellerId(rs.getLong("seller_id"));
            dto.setQuantity(rs.getInt("quantity"));
            dto.setSellPrice(rs.getBigDecimal("sell_price"));
            dto.setSupplyPrice(rs.getBigDecimal("supply_price"));
            dto.setCommissionRate(rs.getDouble("commission_rate"));
            dto.setTaxType(TaxType.valueOf(rs.getString("tax_type")));
            dto.setPromotionDiscountAmount(rs.getBigDecimal("promotion_discount_amount"));
            dto.setCouponDiscountAmount(rs.getBigDecimal("coupon_discount_amount"));
            dto.setPointUsedAmount(rs.getBigDecimal("point_used_amount"));
            dto.setDefaultDeliveryAmount(rs.getBigDecimal("default_delivery_amount"));

            Long dailySettlementId = rs.getLong("settlement_id");
            if (rs.wasNull()) {
                dto.setDailySettlementId(null);
            } else {
                dto.setDailySettlementId(dailySettlementId);
            }
            return dto;
        };

        return new JdbcPagingItemReaderBuilder<OrderProductDTO>()
                .name("optimizedDailySettlementItemReader")
                .dataSource(dataSource)
                .queryProvider(queryProvider)
                .parameterValues(parameterValues)
                .pageSize(CHUNK_SIZE)
                .rowMapper(rowMapper)
                .build();
    }

    @Bean
    public ItemProcessor<OrderProductDTO, DailySettlementDetail> dailyPlusSettlementItemProcessor() {
        return (orderProductDto) -> {
            PositiveDailySettlementCollection collection = new PositiveDailySettlementCollection(orderProductDto, dailySettlementRepository);
            return collection.getDailySettlementDetail();
        };
    }

    @Bean
    public ItemWriter<DailySettlementDetail> dailyPlusSettlementItemWriter() {
        return new ItemWriter<DailySettlementDetail>() {
            @Autowired
            private JdbcTemplate jdbcTemplate;

            @Override
            public void write(Chunk<? extends DailySettlementDetail> items) throws Exception {
                if (items.isEmpty()) {
                    return;
                }

                // 1. ID 기준으로 존재 여부 확인을 위한 쿼리 준비
                List<Object[]> idParams = items.getItems()
                        .stream()
                        .map(detail -> new Object[] {
                                detail.getDailySettlement().getSettlementId(),
                                detail.getOrderProductId(),
                                detail.getSettlementStatus().name()
                        })
                        .toList();

                // 2. 단일 IN 쿼리로 모든 항목의 존재 여부 확인
                StringBuilder existCheckSql = new StringBuilder(
                        "SELECT daily_settlement_id, order_product_id FROM daily_settlement_detail " +
                        "WHERE (daily_settlement_id, order_product_id, settlement_status) IN ("
                );
                for (int i = 0; i < idParams.size(); i++) {
                    if (i > 0) {
                        existCheckSql.append(",");
                    }
                    existCheckSql.append("(?,?,?)");
                }
                existCheckSql.append(")");

                List<Object> flatParams = idParams.stream()
                        .flatMap(Arrays::stream)
                        .collect(Collectors.toList());

                Set<String> existingKeys = new HashSet<>();
                jdbcTemplate.query(existCheckSql.toString(), flatParams.toArray(), rs -> {
                    String key = rs.getLong("daily_settlement_id") + "_" + rs.getLong("order_product_id");
                    existingKeys.add(key);
                });

                // 3. INSERT와 UPDATE로 분리
                List<DailySettlementDetail> toInsert = new ArrayList<>();
                List<DailySettlementDetail> toUpdate = new ArrayList<>();

                for (DailySettlementDetail detail : items) {
                    String key = detail.getDailySettlement().getSettlementId() + "_" + detail.getOrderProductId();
                    if (existingKeys.contains(key)) {
                        toUpdate.add(detail);
                    } else {
                        toInsert.add(detail);
                    }
                }

                // 4. INSERT 쿼리 실행
                if (!toInsert.isEmpty()) {
                    String insertSql = """
                    INSERT INTO daily_settlement_detail (
                        daily_settlement_id,
                        product_id,
                        order_product_id,
                        order_id,
                        quantity,
                        settlement_status,
                        tax_type,
                        tax_amount,
                        sales_amount,
                        promotion_discount_amount,
                        coupon_discount_amount,
                        point_used_amount,
                        shipping_fee,
                        claim_shipping_fee,
                        commission_amount,
                        settlement_amount,
                        created_at,
                        updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

                    jdbcTemplate.batchUpdate(insertSql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            DailySettlementDetail detail = toInsert.get(i);
                            LocalDateTime now = LocalDateTime.now();
                            int idx = 1;

                            ps.setLong(idx++, detail.getDailySettlement().getSettlementId());
                            ps.setLong(idx++, detail.getProductId());
                            ps.setLong(idx++, detail.getOrderProductId());
                            ps.setLong(idx++, detail.getOrderId());
                            ps.setInt(idx++, detail.getQuantity());
                            ps.setString(idx++, detail.getSettlementStatus().name());
                            ps.setString(idx++, detail.getTaxType().name());
                            ps.setBigDecimal(idx++, detail.getTaxAmount());
                            ps.setBigDecimal(idx++, detail.getSalesAmount());
                            ps.setBigDecimal(idx++, detail.getPromotionDiscountAmount());
                            ps.setBigDecimal(idx++, detail.getCouponDiscountAmount());
                            ps.setBigDecimal(idx++, detail.getPointUsedAmount());
                            ps.setBigDecimal(idx++, detail.getShippingFee());
                            ps.setBigDecimal(idx++, detail.getClaimShippingFee());
                            ps.setBigDecimal(idx++, detail.getCommissionAmount());
                            ps.setBigDecimal(idx++, detail.getSettlementAmount());
                            ps.setTimestamp(idx++, Timestamp.valueOf(now));
                            ps.setTimestamp(idx++, Timestamp.valueOf(now));
                        }

                        @Override
                        public int getBatchSize() {
                            return toInsert.size();
                        }
                    });
                }

                // 5. UPDATE 쿼리 실행
                if (!toUpdate.isEmpty()) {
                    String updateSql = """
                    UPDATE daily_settlement_detail SET
                        quantity = ?,
                        settlement_status = ?,
                        tax_type = ?,
                        tax_amount = ?,
                        sales_amount = ?,
                        promotion_discount_amount = ?,
                        coupon_discount_amount = ?,
                        point_used_amount = ?,
                        shipping_fee = ?,
                        claim_shipping_fee = ?,
                        commission_amount = ?,
                        settlement_amount = ?,
                        updated_at = ?
                    WHERE daily_settlement_id = ? 
                    AND order_product_id = ?
                    AND settlement_status = ?
                """;

                    jdbcTemplate.batchUpdate(updateSql, new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            DailySettlementDetail detail = toUpdate.get(i);
                            LocalDateTime now = LocalDateTime.now();
                            int idx = 1;

                            // SET 절 파라미터
                            ps.setInt(idx++, detail.getQuantity());
                            ps.setString(idx++, detail.getSettlementStatus().name());
                            ps.setString(idx++, detail.getTaxType().name());
                            ps.setBigDecimal(idx++, detail.getTaxAmount());
                            ps.setBigDecimal(idx++, detail.getSalesAmount());
                            ps.setBigDecimal(idx++, detail.getPromotionDiscountAmount());
                            ps.setBigDecimal(idx++, detail.getCouponDiscountAmount());
                            ps.setBigDecimal(idx++, detail.getPointUsedAmount());
                            ps.setBigDecimal(idx++, detail.getShippingFee());
                            ps.setBigDecimal(idx++, detail.getClaimShippingFee());
                            ps.setBigDecimal(idx++, detail.getCommissionAmount());
                            ps.setBigDecimal(idx++, detail.getSettlementAmount());
                            ps.setTimestamp(idx++, Timestamp.valueOf(now));

                            // WHERE 절 파라미터
                            ps.setLong(idx++, detail.getDailySettlement().getSettlementId());
                            ps.setLong(idx++, detail.getOrderProductId());
                            ps.setString(idx++, detail.getSettlementStatus().name());
                        }

                        @Override
                        public int getBatchSize() {
                            return toUpdate.size();
                        }
                    });
                }
            }
        };
    }

}
