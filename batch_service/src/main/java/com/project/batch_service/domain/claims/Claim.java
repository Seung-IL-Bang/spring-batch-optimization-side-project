package com.project.batch_service.domain.claims;

import com.project.batch_service.domain.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Claim extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long claimId;

    private Long customerId;

    private Long orderProductId;

    @Enumerated(EnumType.STRING)
    private ClaimType claimType;

    @Enumerated(EnumType.STRING)
    private ClaimStatus claimStatus = ClaimStatus.RECEIVED;

    @Enumerated(EnumType.STRING)
    private ExtraFeePayer extraFeePayer;

    private String claimReason;

    private LocalDateTime completedAt;

    public void completeClaim() {
        completedAt = LocalDateTime.now();
        claimStatus = ClaimStatus.COMPLETED;
    }
}
