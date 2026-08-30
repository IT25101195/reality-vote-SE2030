package com.slit.realityvote.service;

import com.slit.realityvote.entity.JudgeAssignment;

import java.util.List;

public interface JudgeAssignmentService {

    JudgeAssignment assignJudge(Long judgeId, Long showId, Long seasonId, Long episodeId, String panelName);

    List<JudgeAssignment> getAssignmentsForJudge(Long judgeId);

    List<JudgeAssignment> getAssignmentsForShow(Long showId);

    void removeAssignment(Long assignmentId);
}
