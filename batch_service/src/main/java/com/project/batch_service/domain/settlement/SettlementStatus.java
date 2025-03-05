package com.project.batch_service.domain.settlement;

public enum SettlementStatus {

    PENDING("정산대기"),
    PROCESSING("정산처리중"),
    COMPLETED("정산완료"),
    FAILED("정산실패"),
    CANCELLED("정산취소");

    private final String description;

    SettlementStatus(String description) {
        this.description = description;
    }

}
