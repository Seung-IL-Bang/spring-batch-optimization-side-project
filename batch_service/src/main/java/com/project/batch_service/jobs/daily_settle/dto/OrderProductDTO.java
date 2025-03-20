package com.project.batch_service.jobs.daily_settle.dto;

import com.project.batch_service.domain.products.TaxType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderProductDTO {

    private Long orderProductId;
    private Long orderId;
    private Long productId;
    private Long sellerId;
    private Long dailySettlementId;
    private Integer quantity;
    private TaxType taxType;
    private BigDecimal sellPrice;
    private BigDecimal supplyPrice;
    private double commissionRate;
    private BigDecimal pointUsedAmount;
    private BigDecimal promotionDiscountAmount;
    private BigDecimal couponDiscountAmount;
    private BigDecimal defaultDeliveryAmount;

}
