package com.slit.realityvote.service;

import com.slit.realityvote.dto.VoteTallyView;
import com.slit.realityvote.entity.Vote;

import java.util.List;

public interface VoteService {

    /** Casts a vote; throws IllegalStateException if the rules below are violated. */
    Vote castVote(Long sessionId, Long contestantId, Long voterId);

    List<VoteTallyView> getLiveResults(Long sessionId);

    List<Vote> getVotingHistoryForVoter(Long voterId);
}
