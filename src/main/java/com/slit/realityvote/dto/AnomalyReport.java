package com.slit.realityvote.dto;

import java.util.List;

/**
 * Result of pattern-based anomaly detection for one voting session (PBI-14,
 * PBI-15). Lists suspicious actors and human-readable anomaly descriptions
 * so the Compliance Officer can raise a security alert in one click.
 */
public record AnomalyReport(
        Long sessionId,
        String sessionDescription,
        int totalFlaggedEvents,
        List<String> topSuspiciousActors,
        List<String> anomalyDescriptions
) {}
