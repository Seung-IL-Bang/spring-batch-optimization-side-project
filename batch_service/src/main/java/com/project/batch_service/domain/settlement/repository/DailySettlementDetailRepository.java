package com.project.batch_service.domain.settlement.repository;

import com.project.batch_service.domain.settlement.DailySettlementDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailySettlementDetailRepository extends JpaRepository<DailySettlementDetail, Long> {
}
