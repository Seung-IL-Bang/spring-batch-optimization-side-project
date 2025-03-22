package com.project.batch_service.jobs.daily_settle.steps;

import com.project.batch_service.domain.orders.OrderProduct;
import com.project.batch_service.jobs.JobParameterUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
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

@Configuration
@RequiredArgsConstructor
public class PurchaseConfirmStepConfig {

    private final DataSource dataSource;

    @Bean
    @StepScope
    public JdbcCursorItemReader<OrderProduct> deliveryCompletedJpaItemReader(
            @Value("#{jobParameters['settlementDate']}") String settlementDateStr) {

        LocalDate date = JobParameterUtils.parseSettlementDate(settlementDateStr);
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.plusDays(1).atStartOfDay();

        // SQL 쿼리 작성
        String sql = """
            SELECT op.*
            FROM order_product op
            LEFT JOIN claim cl ON op.order_product_id = cl.order_product_id
            WHERE op.delivery_completed_at BETWEEN ? AND ?
            AND op.delivery_status = 'DELIVERED'
            AND op.purchase_confirmed_at IS NULL
            AND (cl.claim_id IS NULL OR cl.completed_at IS NOT NULL)
            ORDER BY op.order_product_id ASC
            """;

        // PreparedStatement 파라미터 설정
        Object[] parameters = new Object[] {
                startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        };

        return new JdbcCursorItemReaderBuilder<OrderProduct>()
                .name("deliveryCompletedJdbcItemReader")
                .dataSource(dataSource)
                .sql(sql)
                .preparedStatementSetter(new ArgumentPreparedStatementSetter(parameters))
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
