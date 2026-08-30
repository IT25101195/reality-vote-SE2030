package com.slit.realityvote.entity;

/**
 * Filing status for a persisted ComplianceReportRecord.
 * DRAFT → FILED is one-way: a filed report cannot be retracted.
 */
public enum ReportStatus {
    DRAFT,
    FILED
}
