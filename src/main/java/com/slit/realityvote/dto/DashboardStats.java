package com.slit.realityvote.dto;

/** Top-line numbers for the Reporting Manager's dashboard. */
public record DashboardStats(long totalShows, long totalContestants, long totalVotes,
                              long totalVoters, long openSessions, long flaggedSecurityEvents) {
}
