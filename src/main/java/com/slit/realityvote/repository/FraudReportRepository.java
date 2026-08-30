package com.slit.realityvote.repository;

import com.slit.realityvote.entity.FraudReport;
import com.slit.realityvote.entity.FraudReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FraudReportRepository extends JpaRepository<FraudReport, Long> {
    List<FraudReport> findBySessionId(Long sessionId);
    List<FraudReport> findAllByOrderByCreatedDateDesc();
    List<FraudReport> findByStatus(FraudReportStatus status);
}
