package com.slit.realityvote.dto;

import java.util.List;
import java.util.Map;

/**
 * Near-real-time voting throughput snapshot for a single VotingSession.
 *
 * contestantStats — richer per-contestant list that drives the Highcharts
 *   chart; includes id, status, biography, and the anomaly-flag so the
 *   chart can colour bars without a second API call.
 *
 * votesByContestantName — kept for backward compatibility with the
 *   compliance/report.html template which iterates a simple name→count map.
 */
public record VotingActivityStats(
        Long sessionId,
        String sessionDescription,
        long totalVotes,
        long totalRejected,
        Map<String, Long> votesByContestantName,
        List<ContestantVoteStat> contestantStats
) {}
