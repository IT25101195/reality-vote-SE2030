package com.slit.realityvote.service.impl;

import com.slit.realityvote.dto.DashboardStats;
import com.slit.realityvote.dto.RankingRow;
import com.slit.realityvote.entity.Contestant;
import com.slit.realityvote.entity.VotingSessionStatus;
import com.slit.realityvote.repository.*;
import com.slit.realityvote.service.ReportsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ReportsServiceImpl implements ReportsService {

    private final RealityShowRepository showRepository;
    private final ContestantRepository contestantRepository;
    private final VoteRepository voteRepository;
    private final VotingSessionRepository sessionRepository;
    private final AuditLogRepository auditLogRepository;
    private final ScoreRepository scoreRepository;

    @Override
    public DashboardStats getDashboardStats() {
        return new DashboardStats(
                showRepository.findByDeletedFalse().size(),
                contestantRepository.search(null, null, null, Pageable.unpaged()).getTotalElements(),
                voteRepository.count(),
                voteRepository.countDistinctVoters(),
                sessionRepository.findByStatusOrderByStartTimeAsc(VotingSessionStatus.OPEN).size(),
                auditLogRepository.countByFlaggedTrue()
        );
    }

    @Override
    public List<RankingRow> getRankingsForShow(Long showId) {
        List<Contestant> contestants = contestantRepository
                .search(null, showId, null, Pageable.unpaged())
                .getContent();

        Map<Long, Long> votesByContestantId = voteRepository.tallyByShow(showId).stream()
                .collect(Collectors.toMap(
                        VoteRepository.ContestantTally::getContestantId,
                        VoteRepository.ContestantTally::getVoteCount));

        // "Combine Judge Scores with Audience Votes" - average judge score
        // per contestant across the whole show.
        Map<Long, Double> avgJudgeScoreByContestantId = scoreRepository.avgScoreByShow(showId).stream()
                .collect(Collectors.toMap(
                        ScoreRepository.ContestantAvgScore::getContestantId,
                        ScoreRepository.ContestantAvgScore::getAvgScore));

        long maxVotes = votesByContestantId.values().stream().mapToLong(Long::longValue).max().orElse(0L);

        List<Contestant> sorted = contestants.stream()
                .sorted(Comparator.comparingDouble((Contestant c) ->
                        combinedScore(c, votesByContestantId, avgJudgeScoreByContestantId, maxVotes)).reversed())
                .toList();

        return IntStream.range(0, sorted.size())
                .mapToObj(i -> {
                    Contestant c = sorted.get(i);
                    long votes = votesByContestantId.getOrDefault(c.getId(), 0L);
                    Double avgJudgeScore = avgJudgeScoreByContestantId.get(c.getId());
                    double combined = combinedScore(c, votesByContestantId, avgJudgeScoreByContestantId, maxVotes);
                    return new RankingRow(i + 1, c.getId(), c.getFullName(), c.getTalentCategory(),
                            c.getStatus().name(), votes, avgJudgeScore, combined);
                })
                .toList();
    }

    /**
     * Votes are normalised to a 0-100 scale (relative to the show's
     * highest vote count) so they're on the same footing as a 0-100
     * judge score before blending 50/50. With no judge scores yet, this
     * reduces to plain normalised-vote ranking - same contestant order
     * as votes alone would give.
     */
    private double combinedScore(Contestant c, Map<Long, Long> votesByContestantId,
                                  Map<Long, Double> avgJudgeScoreByContestantId, long maxVotes) {
        long votes = votesByContestantId.getOrDefault(c.getId(), 0L);
        double normalisedVotes = maxVotes == 0 ? 0 : (votes * 100.0 / maxVotes);
        Double avgJudgeScore = avgJudgeScoreByContestantId.get(c.getId());
        if (avgJudgeScore == null) {
            return normalisedVotes;
        }
        return (normalisedVotes * 0.5) + (avgJudgeScore * 0.5);
    }
}
