package com.project.batch_service.domain.claims;

public enum ExtraFeePayer {
    SELLER("판매자"),
    PLATFORM("플랫폼"),
    CUSTOMER("고객");

    private String description;

    ExtraFeePayer(String description) {
        this.description = description;
    }
}