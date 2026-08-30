package com.slit.realityvote.dto;

import com.slit.realityvote.entity.ContestantStatus;

/**
 * A single contestant's vote tally enriched with profile fields,
 * used to drive the Highcharts activity chart and the click-to-profile
 * card on GET /compliance/activity.
 *
 * flaggedByAnomaly — true when this contestant's ID appears in the
 * AnomalyReport for the same session, which turns the bar red.
 */
public record ContestantVoteStat(
        Long id,
        String fullName,
        long voteCount,
        ContestantStatus status,
        String biography,
        String talentCategory,
        String hometown,
        boolean flaggedByAnomaly
) {
    /**
     * Bar colour used by the Highcharts chart:
     *   red   — flagged by anomaly detection
     *   amber — ELIMINATED or WITHDRAWN (still in session but out)
     *   teal  — ACTIVE / WINNER (clean)
     */
    public String barColor() {
        if (flaggedByAnomaly)                                      return "#ff5c72";  // rv-danger
        if (status == ContestantStatus.ELIMINATED
                || status == ContestantStatus.WITHDRAWN)           return "#ffc857";  // rv-warning
        return "#00e5ff";                                          // rv-cyan
    }
}
