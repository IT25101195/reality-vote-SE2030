package com.slit.realityvote.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full audit-ready compliance report for a single voting session (PBI-14).
 * Combines activity stats, anomaly detection, and integrity verification
 * into one exportable document.
 */
public record ComplianceReportDto(
        Long sessionId,
        String sessionDescription,
        LocalDateTime reportGeneratedAt,
        VotingActivityStats activityStats,
        AnomalyReport anomalyReport,
        IntegrityReport integrityReport,
        List<String> recommendations
) {}
