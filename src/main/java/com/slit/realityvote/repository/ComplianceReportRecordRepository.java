package com.slit.realityvote.repository;

import com.slit.realityvote.entity.ComplianceReportRecord;
import com.slit.realityvote.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplianceReportRecordRepository
        extends JpaRepository<ComplianceReportRecord, Long> {
    List<ComplianceReportRecord> findBySessionId(Long sessionId);
    List<ComplianceReportRecord> findAllByOrderByCreatedDateDesc();
    List<ComplianceReportRecord> findByStatus(ReportStatus status);
}
