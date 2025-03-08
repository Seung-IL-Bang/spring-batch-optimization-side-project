package com.project.batch_service.domain.orders;

import com.project.batch_service.domain.BaseEntity;
import com.project.batch_service.domain.products.Products;
import com.project.batch_service.domain.products.TaxType;
import com.project.batch_service.domain.seller.Seller;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_product_snapshot")
@Getter
public class OrderProductSnapshot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderProductSnapshotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Products product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private Seller seller;

    private int quantity;

    private BigDecimal sellPrice = BigDecimal.ZERO;

    private BigDecimal supplyPrice = BigDecimal.ZERO;

    private BigDecimal promotionDiscountAmount = BigDecimal.ZERO;

    private BigDecimal pointUsedAmount = BigDecimal.ZERO;

    private BigDecimal couponDiscountAmount = BigDecimal.ZERO;

    private BigDecimal defaultDeliveryAmount = BigDecimal.ZERO;

    private int taxRate;

    @Enumerated(EnumType.STRING)
    private TaxType taxType;
}
