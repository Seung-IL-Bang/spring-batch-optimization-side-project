package com.project.batch_service.domain.settlement;

import com.project.batch_service.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_daily_settlement_seller_date",
                        columnNames = {"sellerId", "settlementDate"}
                )
        }
)
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailySettlement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long settlementId;

    private Long sellerId;

    private LocalDate settlementDate;

    private int totalOrderCount;

    private int totalClaimCount;

    private int totalProductCount;

    private int totalQuantity;

    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal salesAmount = BigDecimal.ZERO;
    private BigDecimal promotionDiscountAmount = BigDecimal.ZERO;
    private BigDecimal couponDiscountAmount = BigDecimal.ZERO;
    private BigDecimal pointUsedAmount = BigDecimal.ZERO;
    private BigDecimal shippingFee = BigDecimal.ZERO;
    private BigDecimal claimShippingFee = BigDecimal.ZERO;
    private BigDecimal commissionAmount = BigDecimal.ZERO;
    private BigDecimal TotalSettlementAmount = BigDecimal.ZERO;
}
