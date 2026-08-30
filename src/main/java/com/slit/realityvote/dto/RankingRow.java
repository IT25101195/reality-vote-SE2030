package com.slit.realityvote.dto;

/**
 * One row of the contestant ranking report, already sorted and numbered.
 *
 * avgJudgeScore is null when no judge has scored this contestant yet -
 * the report still works for shows that haven't started judging.
 * combinedScore is what the row is actually ranked by: audience votes
 * normalised to a 0-100 scale (so they're comparable to a 0-100 judge
 * score) blended 50/50 with avgJudgeScore when judge scores exist,
 * otherwise it's just the normalised vote score - i.e. rankings behave
 * exactly as before for shows with no judging data.
 */
public record RankingRow(int rank, Long contestantId, String contestantName,
                          String talentCategory, String status, long totalVotes,
                          Double avgJudgeScore, double combinedScore) {
}
