package com.project.batch_service.domain.products;

import com.project.batch_service.domain.BaseEntity;
import com.project.batch_service.domain.seller.Seller;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Getter
public class Products extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private Seller seller;

    private String productName;

    @Enumerated(EnumType.STRING)
    private ProductCategory productCategory;

    @Enumerated(EnumType.STRING)
    private TaxType taxType;

    private BigDecimal sellPrice = BigDecimal.ZERO;

    private BigDecimal supplyPrice = BigDecimal.ZERO;

}
