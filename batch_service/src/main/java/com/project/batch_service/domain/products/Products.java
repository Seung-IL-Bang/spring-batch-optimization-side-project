package com.project.batch_service.domain.products;

import com.project.batch_service.domain.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
public class Products extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    private Long sellerId;

    private String productName;

    @Enumerated(EnumType.STRING)
    private ProductCategory productCategory;

    @Enumerated(EnumType.STRING)
    private TaxType taxType;

    private BigDecimal sellPrice = BigDecimal.ZERO;

    private BigDecimal supplyPrice = BigDecimal.ZERO;

}
