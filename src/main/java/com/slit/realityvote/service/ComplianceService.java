package com.slit.realityvote.service;

import com.slit.realityvote.dto.AnomalyReport;
import com.slit.realityvote.dto.ComplianceDashboardStats;
import com.slit.realityvote.dto.ComplianceReportDto;
import com.slit.realityvote.dto.IntegrityReport;
import com.slit.realityvote.dto.VotingActivityStats;

/**
 * Module 6.4 — Voting Compliance & Security service interface.
 *
 * Separation of duties: every method here is read-only with respect to
 * Votes and AuditLogs. There is deliberately no method that creates,
 * updates, or deletes a Vote or an AuditLog entry — the write restriction
 * is enforced at the service layer, not just in the UI.
 */
public interface ComplianceService {

    /** Aggregate KPIs for the dashboard landing page. */
    ComplianceDashboardStats getDashboardStats();

    /**
     * Near-real-time voting throughput and per-contestant counts
     * for one session (GET /compliance/activity).
     */
    VotingActivityStats getActivity(Long sessionId);

    /**
     * Flag-based anomaly detection: surfaces suspicious actors and
     * describes the patterns that triggered the flag (GET /compliance/anomalies).
     */
    AnomalyReport detectAnomalies(Long sessionId);

    /**
     * Reconciles the stored Vote-table count vs. the audit-log record of
     * VOTE_CAST events to verify integrity (GET /compliance/verify/{sessionId}).
     */
    IntegrityReport verifyIntegrity(Long sessionId);

    /**
     * Produces the full audit-ready compliance report for a session,
     * combining activity, anomaly detection, and integrity verification
     * (GET /compliance/reports/{sessionId}).
     */
    ComplianceReportDto generateReport(Long sessionId);
}
