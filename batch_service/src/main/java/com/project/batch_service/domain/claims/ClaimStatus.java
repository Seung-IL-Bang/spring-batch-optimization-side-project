package com.project.batch_service.domain.claims;

public enum ClaimStatus {

    WITHDRAW("철회"),
    RECEIVED("접수"),
    IN_PROGRESS("처리 중"),
    COMPLETED("처리 완료");

    private String description;

    ClaimStatus(String description) {
        this.description = description;
    }

}
