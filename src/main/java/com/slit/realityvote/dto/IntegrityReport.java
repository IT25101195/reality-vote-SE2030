package com.slit.realityvote.dto;

/**
 * Vote integrity check: reconciles the stored aggregate count from the
 * Vote table against the audit log record of VOTE_CAST events, surfacing
 * any discrepancy as a WARNING or CRITICAL status (PBI-16).
 */
public record IntegrityReport(
        Long sessionId,
        String sessionDescription,
        long storedVoteCount,
        long auditedVoteCount,
        long rejectedVoteCount,
        boolean countsMatch,
        String integrityStatus          // OK | WARNING | CRITICAL
) {}
