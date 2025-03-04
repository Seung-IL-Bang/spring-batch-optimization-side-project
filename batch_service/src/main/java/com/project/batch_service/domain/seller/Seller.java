package com.project.batch_service.domain.seller;

import com.project.batch_service.domain.BaseEntity;
import jakarta.persistence.*;

@Entity
public class Seller extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seller_id")
    private int id;

    @Column(length = 50)
    private String sellerName;

    private String businessNo;

    private int defaultDeliveryAmount;

    private double commissionRate;
}
