package com.slit.realityvote.repository;

import com.slit.realityvote.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    // Duplicate-vote guard used by VoteServiceImpl before saving
    boolean existsByVotingSession_IdAndContestant_IdAndVoter_Id(Long sessionId, Long contestantId, Long voterId);

    long countByVotingSession_IdAndContestant_Id(Long sessionId, Long contestantId);

    long countByVotingSession_Id(Long sessionId);

    /**
     * Live tally: contestant id + vote count, ordered highest first.
     * Backs both the "Live Vote Counter" requirement and (later) the
     * Reports module's ranking report.
     */
    @Query("SELECT v.contestant.id AS contestantId, COUNT(v) AS voteCount " +
           "FROM Vote v WHERE v.votingSession.id = :sessionId " +
           "GROUP BY v.contestant.id ORDER BY COUNT(v) DESC")
    List<ContestantTally> tallyBySession(@Param("sessionId") Long sessionId);

    interface ContestantTally {
        Long getContestantId();
        Long getVoteCount();
    }

    // Did this viewer vote for this specific contestant in this session?
    List<Vote> findByVotingSession_IdAndVoter_Id(Long sessionId, Long voterId);

    // Full voting history for a viewer's "My Voting History" page
    List<Vote> findByVoter_IdOrderByVotedAtDesc(Long voterId);

    // ---- Reports & Analytics ----

    @Query("SELECT COUNT(DISTINCT v.voter.id) FROM Vote v")
    long countDistinctVoters();

    /**
     * Total votes per contestant across ALL of that contestant's voting
     * sessions within one show - i.e. their overall standing, not just
     * one session's tally. Backs the Contestant Rankings / Winner report.
     */
    @Query("SELECT v.contestant.id AS contestantId, COUNT(v) AS voteCount " +
           "FROM Vote v WHERE v.contestant.show.id = :showId " +
           "GROUP BY v.contestant.id ORDER BY COUNT(v) DESC")
    List<ContestantTally> tallyByShow(@Param("showId") Long showId);
}
