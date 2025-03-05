package com.project.batch_service.domain.seller;

import com.project.batch_service.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Getter
public class Seller extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seller_id")
    private Long sellerId;

    @Column(length = 50)
    private String sellerName;

    private String businessNo;

    private BigDecimal defaultDeliveryAmount;

    private double commissionRate;
}
