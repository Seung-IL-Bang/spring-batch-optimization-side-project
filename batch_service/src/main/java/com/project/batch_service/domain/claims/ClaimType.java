package com.project.batch_service.domain.claims;

public enum ClaimType {

    RETURN("반품"),
    REFUND("환불"),
    EXCHANGE("교환"),
    CANCELLATION("취소");

    private String description;

    ClaimType(String description) {
        this.description = description;
    }
}
