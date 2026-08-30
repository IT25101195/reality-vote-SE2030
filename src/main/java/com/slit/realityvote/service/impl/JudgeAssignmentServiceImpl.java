package com.slit.realityvote.service.impl;

import com.slit.realityvote.entity.Episode;
import com.slit.realityvote.entity.Judge;
import com.slit.realityvote.entity.JudgeAssignment;
import com.slit.realityvote.entity.RealityShow;
import com.slit.realityvote.entity.Season;
import com.slit.realityvote.repository.EpisodeRepository;
import com.slit.realityvote.repository.JudgeAssignmentRepository;
import com.slit.realityvote.repository.JudgeRepository;
import com.slit.realityvote.repository.RealityShowRepository;
import com.slit.realityvote.repository.SeasonRepository;
import com.slit.realityvote.service.JudgeAssignmentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implements "Assign Judges to Shows/Seasons/Episodes" and "Create and
 * Manage Judging Panels". A judge can be assigned at three scopes -
 * whole show, one season, or one episode - by leaving seasonId/episodeId
 * null; ScoreServiceImpl walks all three scopes to work out which
 * contestants a judge can currently score.
 */
@Service
@RequiredArgsConstructor
public class JudgeAssignmentServiceImpl implements JudgeAssignmentService {

    private final JudgeAssignmentRepository assignmentRepository;
    private final JudgeRepository judgeRepository;
    private final RealityShowRepository showRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;

    @Override
    @Transactional
    public JudgeAssignment assignJudge(Long judgeId, Long showId, Long seasonId, Long episodeId, String panelName) {
        Judge judge = judgeRepository.findByIdAndDeletedFalse(judgeId)
                .orElseThrow(() -> new EntityNotFoundException("Judge not found with id: " + judgeId));
        RealityShow show = showRepository.findByIdAndDeletedFalse(showId)
                .orElseThrow(() -> new EntityNotFoundException("Reality show not found with id: " + showId));

        Season season = null;
        if (seasonId != null) {
            season = seasonRepository.findById(seasonId)
                    .orElseThrow(() -> new EntityNotFoundException("Season not found with id: " + seasonId));
        }

        Episode episode = null;
        if (episodeId != null) {
            episode = episodeRepository.findById(episodeId)
                    .orElseThrow(() -> new EntityNotFoundException("Episode not found with id: " + episodeId));
        }

        if (assignmentRepository.existsByJudge_IdAndShow_IdAndSeason_IdAndEpisode_Id(
                judgeId, showId, seasonId, episodeId)) {
            throw new IllegalArgumentException("This judge is already assigned to that scope.");
        }

        JudgeAssignment assignment = JudgeAssignment.builder()
                .judge(judge)
                .show(show)
                .season(season)
                .episode(episode)
                .panelName(panelName)
                .build();

        return assignmentRepository.save(assignment);
    }

    @Override
    public List<JudgeAssignment> getAssignmentsForJudge(Long judgeId) {
        return assignmentRepository.findByJudge_IdOrderByAssignedDateDesc(judgeId);
    }

    @Override
    public List<JudgeAssignment> getAssignmentsForShow(Long showId) {
        return assignmentRepository.findByShow_IdOrderByAssignedDateDesc(showId);
    }

    @Override
    @Transactional
    public void removeAssignment(Long assignmentId) {
        if (!assignmentRepository.existsById(assignmentId)) {
            throw new EntityNotFoundException("Assignment not found with id: " + assignmentId);
        }
        assignmentRepository.deleteById(assignmentId);
    }
}
