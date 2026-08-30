package com.slit.realityvote.repository;

import com.slit.realityvote.entity.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScoreRepository extends JpaRepository<Score, Long> {

    Optional<Score> findByJudge_IdAndContestant_IdAndEpisode_Id(Long judgeId, Long contestantId, Long episodeId);

    List<Score> findByJudge_IdOrderBySubmittedDateDesc(Long judgeId);

    List<Score> findByEpisode_Id(Long episodeId);

    List<Score> findByContestant_Id(Long contestantId);

    /**
     * Average judge score per contestant across an entire show - backs
     * "Combine Judge Scores with Audience Votes" in the Reports module
     * (ReportsServiceImpl.getRankingsForShow).
     */
    @Query("SELECT s.contestant.id AS contestantId, AVG(s.scoreValue) AS avgScore, COUNT(s) AS scoreCount " +
           "FROM Score s WHERE s.contestant.show.id = :showId " +
           "GROUP BY s.contestant.id")
    List<ContestantAvgScore> avgScoreByShow(@Param("showId") Long showId);

    interface ContestantAvgScore {
        Long getContestantId();
        Double getAvgScore();
        Long getScoreCount();
    }
}
