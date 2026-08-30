package com.slit.realityvote.service;

import com.slit.realityvote.entity.Contestant;
import com.slit.realityvote.entity.Episode;
import com.slit.realityvote.entity.Score;

import java.util.List;

public interface ScoreService {

    /** Episodes the given judge (by login email) currently has a panel assignment covering. */
    List<Episode> getAssignedEpisodes(String judgeEmail);

    /** Contestants the given judge may score for one episode (from the episode's voting-session pool). */
    List<Contestant> getScorableContestants(String judgeEmail, Long episodeId);

    /** Create a new score, or revise an existing one if the judging window is still open. */
    Score submitScore(String judgeEmail, Long contestantId, Long episodeId, Integer scoreValue, String remarks);

    List<Score> getScoringHistory(String judgeEmail);

    Score getExistingScore(String judgeEmail, Long contestantId, Long episodeId);
}
