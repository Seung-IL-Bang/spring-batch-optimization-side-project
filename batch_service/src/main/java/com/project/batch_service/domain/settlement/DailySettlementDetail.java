package com.project.batch_service.domain.settlement;

import com.project.batch_service.domain.BaseEntity;
import com.project.batch_service.domain.products.TaxType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailySettlementDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dailySettlementDetailId;

    private Long dailySettlementId;

    private Long productId;

    private Long orderProductId;

    private Long orderId;

    private int quantity;

    @Enumerated(EnumType.STRING)
    private SettlementStatus settlementStatus;

    @Enumerated(EnumType.STRING)
    private TaxType taxType;

    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal salesAmount = BigDecimal.ZERO;
    private BigDecimal promotionDiscountAmount = BigDecimal.ZERO;
    private BigDecimal couponDiscountAmount = BigDecimal.ZERO;
    private BigDecimal pointUsedAmount = BigDecimal.ZERO;
    private BigDecimal shippingFee = BigDecimal.ZERO;
    private BigDecimal claimShippingFee = BigDecimal.ZERO;
    private BigDecimal commissionAmount = BigDecimal.ZERO;
    private BigDecimal productSettlementAmount = BigDecimal.ZERO;
}
