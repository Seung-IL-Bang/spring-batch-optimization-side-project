package com.project.batch_service.domain.settlement.repository;

import com.project.batch_service.domain.settlement.DailySettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailySettlementRepository extends JpaRepository<DailySettlement, Long> {

    Optional<DailySettlement> findBySellerIdAndSettlementDate(Long sellerId, LocalDate settlementDate);
}
