package com.project.batch_service.domain.orders;

import com.project.batch_service.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
public class OrderProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_product_id")
    private int orderProductId;

    private Long orderId;

    private Long productId;

    private int productCount;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus deliveryStatus;

    private LocalDateTime purchaseConfirmedAt;

    private LocalDateTime deliveryCompletedAt;

    public OrderProduct purchaseConfirm() {
        this.purchaseConfirmedAt = LocalDateTime.now();
        return this;
    }

    public OrderProduct deliveryComplete() {
        this.deliveryCompletedAt = LocalDateTime.now();
        return this;
    }
}
