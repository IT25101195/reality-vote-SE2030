package com.slit.realityvote.repository;

import com.slit.realityvote.entity.JudgeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JudgeAssignmentRepository extends JpaRepository<JudgeAssignment, Long> {

    List<JudgeAssignment> findByJudge_IdOrderByAssignedDateDesc(Long judgeId);

    List<JudgeAssignment> findByShow_IdOrderByAssignedDateDesc(Long showId);

    List<JudgeAssignment> findByJudge_IdAndShow_Id(Long judgeId, Long showId);

    /**
     * Duplicate-assignment guard. Uses IS NULL / :param comparisons rather
     * than a derived-query "AndSeason_IdAndEpisode_Id" method, because in
     * SQL "column = NULL" never matches - a show-level assignment
     * (season/episode both null) needs an explicit null-safe check.
     */
    @Query("SELECT COUNT(a) > 0 FROM JudgeAssignment a WHERE a.judge.id = :judgeId " +
           "AND a.show.id = :showId " +
           "AND (:seasonId IS NULL AND a.season IS NULL OR a.season.id = :seasonId) " +
           "AND (:episodeId IS NULL AND a.episode IS NULL OR a.episode.id = :episodeId)")
    boolean existsByJudge_IdAndShow_IdAndSeason_IdAndEpisode_Id(
            @Param("judgeId") Long judgeId, @Param("showId") Long showId,
            @Param("seasonId") Long seasonId, @Param("episodeId") Long episodeId);

    /**
     * Every panel assignment that could plausibly cover this episode:
     * an exact episode-level assignment, a season-level assignment for
     * the episode's season, or a show-level assignment for the episode's
     * show. Used to work out which judges - and therefore which
     * contestants - a given judge can see and score for an episode.
     */
    List<JudgeAssignment> findByEpisode_Id(Long episodeId);

    List<JudgeAssignment> findBySeason_Id(Long seasonId);

    List<JudgeAssignment> findByShow_IdAndSeasonIsNullAndEpisodeIsNull(Long showId);
}
