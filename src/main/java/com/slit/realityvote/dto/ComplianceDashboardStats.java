package com.slit.realityvote.dto;

/**
 * Immutable snapshot of compliance KPIs shown on the Compliance Officer
 * dashboard. All counts are live database aggregates, not cached.
 */
public record ComplianceDashboardStats(
        long totalEvents,
        long flaggedEvents,
        long suspiciousActivityCount,
        long loginFailures,
        long voteRejections,
        long flaggedLast24h
) {}
