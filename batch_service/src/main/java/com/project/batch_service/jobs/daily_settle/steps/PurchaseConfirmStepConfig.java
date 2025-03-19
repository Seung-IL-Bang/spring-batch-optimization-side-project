package com.project.batch_service.jobs.daily_settle.steps;

import com.project.batch_service.domain.orders.OrderProduct;
import com.project.batch_service.domain.orders.repository.OrderProductRepository;
import com.project.batch_service.jobs.JobParameterUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
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
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class PurchaseConfirmStepConfig {

    private final DataSource dataSource;

    @Bean
    @StepScope
    public JdbcPagingItemReader<OrderProduct> deliveryCompletedJpaItemReader(
            @Value("#{jobParameters['settlementDate']}") String settlementDateStr,
            @Value("#{jobParameters['chunkSize']}") Integer chunkSize) throws Exception {

        int CHUNK_SIZE = JobParameterUtils.parseChunkSize(chunkSize);
        LocalDate date = JobParameterUtils.parseSettlementDate(settlementDateStr);
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.plusDays(1).atStartOfDay();

        // 파라미터 설정
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("startTime", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        parameterValues.put("endTime", endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        SqlPagingQueryProviderFactoryBean queryProviderFactory = new SqlPagingQueryProviderFactoryBean();
        queryProviderFactory.setDataSource(dataSource);

        queryProviderFactory.setSelectClause("SELECT op.*");
        queryProviderFactory.setFromClause("FROM order_product op LEFT JOIN claim cl ON op.order_product_id = cl.order_product_id");
        queryProviderFactory.setWhereClause("""
                WHERE op.delivery_completed_at BETWEEN :startTime AND :endTime
                AND op.delivery_status = 'DELIVERED'
                AND op.purchase_confirmed_at IS NULL
                AND (cl.claim_id IS NULL OR cl.completed_at IS NOT NULL)
                """);

        // 정렬 키 설정 - orderProductId로 정렬하여 일관된 페이징
        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("op.order_product_id", Order.ASCENDING);
        queryProviderFactory.setSortKeys(sortKeys);

        PagingQueryProvider queryProvider = queryProviderFactory.getObject();

        return new JdbcPagingItemReaderBuilder<OrderProduct>()
                .name("deliveryCompletedJdbcItemReader")
                .dataSource(dataSource)
                .queryProvider(queryProvider)
                .parameterValues(parameterValues)
                .pageSize(CHUNK_SIZE)
                .rowMapper(new BeanPropertyRowMapper<>(OrderProduct.class))
                .build();
    }

    @Bean
    public ItemWriter<OrderProduct> purchaseConfirmedItemWriter() {
        LocalDateTime now = LocalDateTime.now();
        return new ItemWriter<>() {
            @Autowired
            private JdbcTemplate jdbcTemplate;

            @Override
            public void write(Chunk<? extends OrderProduct> items) throws Exception {
                jdbcTemplate.batchUpdate("UPDATE order_product SET purchase_confirmed_at = ? WHERE order_product_id = ?",
                        new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement ps, int i) throws SQLException {
                                OrderProduct orderProduct = items.getItems().get(i);
                                ps.setTimestamp(1, Timestamp.valueOf(now));
                                ps.setLong(2, orderProduct.getOrderProductId());
                            }

                            @Override
                            public int getBatchSize() {
                                return items.size();
                            }
                        });
            }
        };
    }


}
