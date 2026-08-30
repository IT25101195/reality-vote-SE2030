package com.slit.realityvote.service;

import com.slit.realityvote.entity.ComplianceReportRecord;

import java.util.List;
import java.util.Optional;

/**
 * Service for persisting and filing compliance report snapshots.
 * No delete method — a filed report is an official record.
 */
public interface ComplianceReportRecordService {
    ComplianceReportRecord generate(Long sessionId, String raisedByEmail);
    ComplianceReportRecord file(Long id);
    List<ComplianceReportRecord> getAll();
    Optional<ComplianceReportRecord> getById(Long id);
    List<ComplianceReportRecord> getBySession(Long sessionId);
}
