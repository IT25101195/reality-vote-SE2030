package com.slit.realityvote.service;

import com.slit.realityvote.entity.FraudReport;
import com.slit.realityvote.entity.FraudReportStatus;

import java.util.List;
import java.util.Optional;

/**
 * Service for persisted FraudReport lifecycle.
 * No delete method — fraud reports are evidence.
 */
public interface FraudReportService {
    FraudReport create(Long sessionId, String raisedByEmail);
    FraudReport updateSummary(Long id, String summary);
    FraudReport advance(Long id);   // DRAFT→FILED, FILED→UNDER_REVIEW, UNDER_REVIEW→RESOLVED
    List<FraudReport> getAll();
    Optional<FraudReport> getById(Long id);
    List<FraudReport> getBySession(Long sessionId);
}
