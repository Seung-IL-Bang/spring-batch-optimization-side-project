package com.project.batch_service.domain.settlement.repository;

import com.project.batch_service.domain.settlement.DailySettlement;
import com.project.batch_service.domain.settlement.DailySettlementDetail;
import com.project.batch_service.domain.settlement.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DailySettlementDetailRepository extends JpaRepository<DailySettlementDetail, Long> {

    Optional<DailySettlementDetail> findByDailySettlementAndOrderProductIdAndSettlementStatus(DailySettlement dailySettlement, Long orderProductId, SettlementStatus settlementStatus);
}
