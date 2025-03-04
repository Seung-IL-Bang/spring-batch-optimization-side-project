package com.project.batch_service.domain.orders;

public enum DeliveryStatus {

    PENDING("배송 전"),
    IN_TRANSIT("배송 중"),
    DELIVERED("배송 완료");


    private String description;

    DeliveryStatus(String description) {
        this.description = description;
    }
}

