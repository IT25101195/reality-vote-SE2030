package com.slit.realityvote.service.impl;

import com.slit.realityvote.entity.*;
import com.slit.realityvote.repository.ContestantRepository;
import com.slit.realityvote.repository.EpisodeRepository;
import com.slit.realityvote.repository.VotingSessionRepository;
import com.slit.realityvote.service.AuditLogService;
import com.slit.realityvote.service.VotingSessionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VotingSessionServiceImpl implements VotingSessionService {

    private final VotingSessionRepository sessionRepository;
    private final EpisodeRepository episodeRepository;
    private final ContestantRepository contestantRepository;
    private final AuditLogService auditLogService;

    @Override
    public List<VotingSession> getAllSessions() {
        return sessionRepository.findAllByOrderByStartTimeDesc();
    }

    @Override
    public List<VotingSession> getSessionsByStatus(VotingSessionStatus status) {
        return sessionRepository.findByStatusOrderByStartTimeAsc(status);
    }

    @Override
    public VotingSession getById(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Voting session not found with id: " + id));
    }

    @Override
    @Transactional
    public VotingSession createSession(Long episodeId, List<Long> contestantIds, VotingSession session) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new EntityNotFoundException("Episode not found with id: " + episodeId));

        if (session.getEndTime().isBefore(session.getStartTime())) {
            throw new IllegalArgumentException("End time cannot be before start time");
        }
        if (contestantIds == null || contestantIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one contestant for this voting session");
        }

        List<Contestant> contestants = contestantRepository.findAllById(contestantIds);
        if (contestants.size() != contestantIds.size()) {
            throw new IllegalArgumentException("One or more selected contestants could not be found");
        }

        session.setEpisode(episode);
        session.setContestants(contestants);
        session.setStatus(VotingSessionStatus.SCHEDULED);
        return sessionRepository.save(session);
    }

    @Override
    @Transactional
    public VotingSession openSession(Long id) {
        VotingSession session = getById(id);
        if (session.getStatus() == VotingSessionStatus.CLOSED) {
            throw new IllegalStateException("A closed session cannot be re-opened. Create a new session instead.");
        }
        session.setStatus(VotingSessionStatus.OPEN);
        VotingSession saved = sessionRepository.save(session);
        auditLogService.record(AuditEventType.SESSION_OPENED,
                "Voting session " + id + " opened", currentActorEmail());
        return saved;
    }

    @Override
    @Transactional
    public VotingSession closeSession(Long id) {
        VotingSession session = getById(id);
        session.setStatus(VotingSessionStatus.CLOSED);
        // "What should happen automatically when a voting session ends?"
        // -> stop accepting votes (status=CLOSED makes VoteServiceImpl
        // reject new votes), results are then read straight from Vote
        // rows by the tally query - no separate results table needed.
        VotingSession saved = sessionRepository.save(session);
        auditLogService.record(AuditEventType.SESSION_CLOSED,
                "Voting session " + id + " closed", currentActorEmail());
        return saved;
    }

    @Override
    @Transactional
    public void deleteSession(Long id) {
        VotingSession session = getById(id);
        sessionRepository.delete(session);
        auditLogService.record(AuditEventType.SESSION_DELETED,
                "Voting session " + id + " deleted", currentActorEmail());
    }

    /** Pulls the logged-in admin's username (email) from the security context for audit attribution. */
    private String currentActorEmail() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}