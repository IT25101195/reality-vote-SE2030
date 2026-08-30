package com.slit.realityvote.service.impl;

import com.slit.realityvote.entity.Contestant;
import com.slit.realityvote.entity.Episode;
import com.slit.realityvote.entity.Judge;
import com.slit.realityvote.entity.JudgeAssignment;
import com.slit.realityvote.entity.Score;
import com.slit.realityvote.entity.VotingSession;
import com.slit.realityvote.repository.ContestantRepository;
import com.slit.realityvote.repository.EpisodeRepository;
import com.slit.realityvote.repository.JudgeAssignmentRepository;
import com.slit.realityvote.repository.JudgeRepository;
import com.slit.realityvote.repository.ScoreRepository;
import com.slit.realityvote.repository.VotingSessionRepository;
import com.slit.realityvote.service.ScoreService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Backs the judge-facing side of Judge & Panel Management: seeing which
 * contestants are assigned to a judge for scoring, and submitting /
 * revising scores. A JudgeAssignment can cover an episode directly, an
 * entire season, or an entire show - this class resolves all three
 * scopes down to a concrete list of episodes and contestants.
 */
@Service
@RequiredArgsConstructor
public class ScoreServiceImpl implements ScoreService {

    private final ScoreRepository scoreRepository;
    private final JudgeRepository judgeRepository;
    private final JudgeAssignmentRepository assignmentRepository;
    private final EpisodeRepository episodeRepository;
    private final VotingSessionRepository votingSessionRepository;
    private final ContestantRepository contestantRepository;

    private Judge requireJudge(String judgeEmail) {
        return judgeRepository.findByEmailIgnoreCaseAndDeletedFalse(judgeEmail)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No judge profile found for " + judgeEmail + ". Ask an administrator to create one."));
    }

    @Override
    public List<Episode> getAssignedEpisodes(String judgeEmail) {
        Judge judge = requireJudge(judgeEmail);
        List<JudgeAssignment> assignments = assignmentRepository.findByJudge_IdOrderByAssignedDateDesc(judge.getId());

        Set<Episode> episodes = new LinkedHashSet<>();
        for (JudgeAssignment a : assignments) {
            if (a.getEpisode() != null) {
                episodes.add(a.getEpisode());
            } else if (a.getSeason() != null) {
                episodes.addAll(episodeRepository.findBySeasonIdOrderByEpisodeNumberAsc(a.getSeason().getId()));
            } else if (a.getShow() != null) {
                a.getShow().getSeasons().forEach(season ->
                        episodes.addAll(episodeRepository.findBySeasonIdOrderByEpisodeNumberAsc(season.getId())));
            }
        }
        return new ArrayList<>(episodes);
    }

    @Override
    public List<Contestant> getScorableContestants(String judgeEmail, Long episodeId) {
        // Confirms this judge is actually assigned to this episode before
        // revealing any contestant list - enforces panel scope, not just UI hiding.
        List<Episode> assigned = getAssignedEpisodes(judgeEmail);
        Episode episode = assigned.stream()
                .filter(e -> e.getId().equals(episodeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("You are not assigned to judge this episode."));

        // Contestant pool comes from the episode's voting session(s) - the
        // same pool the audience votes on - falling back to every
        // contestant on the show if no session has been created yet.
        List<VotingSession> sessions = votingSessionRepository.findByEpisodeIdOrderByStartTimeDesc(episodeId);
        Set<Contestant> contestants = new LinkedHashSet<>();
        sessions.forEach(s -> contestants.addAll(s.getContestants()));

        if (contestants.isEmpty()) {
            Long showId = episode.getSeason().getShow().getId();
            contestants.addAll(contestantRepository.search(null, showId, null, Pageable.unpaged()).getContent());
        }
        return new ArrayList<>(contestants);
    }

    @Override
    @Transactional
    public Score submitScore(String judgeEmail, Long contestantId, Long episodeId, Integer scoreValue, String remarks) {
        Judge judge = requireJudge(judgeEmail);

        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new EntityNotFoundException("Episode not found with id: " + episodeId));

        Contestant contestant = getScorableContestants(judgeEmail, episodeId).stream()
                .filter(c -> c.getId().equals(contestantId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("This contestant is not scorable by you for this episode."));

        var existing = scoreRepository.findByJudge_IdAndContestant_IdAndEpisode_Id(
                judge.getId(), contestantId, episodeId);

        if (existing.isPresent()) {
            // Revision - only while the judging window is open
            if (!episode.isJudgingOpen()) {
                throw new IllegalStateException("The judging window for this episode is closed; scores can no longer be revised.");
            }
            Score score = existing.get();
            score.setScoreValue(scoreValue);
            score.setRemarks(remarks);
            return scoreRepository.save(score);
        } else {
            if (!episode.isJudgingOpen()) {
                throw new IllegalStateException("The judging window for this episode is not open yet.");
            }
            Score score = Score.builder()
                    .judge(judge)
                    .contestant(contestant)
                    .episode(episode)
                    .scoreValue(scoreValue)
                    .remarks(remarks)
                    .build();
            return scoreRepository.save(score);
        }
    }

    @Override
    public List<Score> getScoringHistory(String judgeEmail) {
        Judge judge = requireJudge(judgeEmail);
        return scoreRepository.findByJudge_IdOrderBySubmittedDateDesc(judge.getId());
    }

    @Override
    public Score getExistingScore(String judgeEmail, Long contestantId, Long episodeId) {
        Judge judge = requireJudge(judgeEmail);
        return scoreRepository.findByJudge_IdAndContestant_IdAndEpisode_Id(judge.getId(), contestantId, episodeId)
                .orElse(null);
    }
}
