package com.slit.realityvote.entity;

/**
 * Lifecycle status for a persisted FraudReport.
 * Once FILED a report cannot be deleted — it is permanent evidence.
 */
public enum FraudReportStatus {
    DRAFT,
    FILED,
    UNDER_REVIEW,
    RESOLVED
}
