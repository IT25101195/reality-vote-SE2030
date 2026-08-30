package com.slit.realityvote.dto;

/** Live results row: one contestant's vote count and share of the session total. */
public record VoteTallyView(Long contestantId, String contestantName, String photoPath,
                             long voteCount, double percentage) {
}
