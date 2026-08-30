package com.slit.realityvote.repository;

import com.slit.realityvote.entity.VotingSession;
import com.slit.realityvote.entity.VotingSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VotingSessionRepository extends JpaRepository<VotingSession, Long> {
    List<VotingSession> findByStatusOrderByStartTimeAsc(VotingSessionStatus status);
    List<VotingSession> findAllByOrderByStartTimeDesc();
    List<VotingSession> findByEpisodeIdOrderByStartTimeDesc(Long episodeId);
}
