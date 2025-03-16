package com.project.batch_service.domain.orders;

import com.project.batch_service.domain.BaseEntity;
import com.project.batch_service.domain.products.Products;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
public class OrderProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_product_id")
    private Long orderProductId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Products product;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_product_snapshot_id")
    private OrderProductSnapshot orderProductSnapshot;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus deliveryStatus;

    private LocalDateTime purchaseConfirmedAt;

    private LocalDateTime deliveryCompletedAt;

    public OrderProduct purchaseConfirm(LocalDateTime now) {
        this.purchaseConfirmedAt = now;
        return this;
    }

    public OrderProduct deliveryComplete() {
        this.deliveryCompletedAt = LocalDateTime.now();
        return this;
    }
}
