package com.slit.realityvote.entity;

/**
 * A Judge's availability status, set by the Administrator. Mirrors the
 * ContestantStatus / ShowStatus pattern used elsewhere in the system.
 * INACTIVE judges are hidden from new panel assignments but their past
 * scores are kept for historical rankings and reports.
 */
public enum JudgeStatus {
    ACTIVE,
    INACTIVE
}
