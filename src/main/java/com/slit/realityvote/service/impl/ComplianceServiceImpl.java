package com.slit.realityvote.service.impl;

import com.slit.realityvote.dto.*;
import com.slit.realityvote.entity.AuditEventType;
import com.slit.realityvote.entity.Contestant;
import com.slit.realityvote.entity.VotingSession;
import com.slit.realityvote.repository.AuditLogRepository;
import com.slit.realityvote.repository.VoteRepository;
import com.slit.realityvote.repository.VotingSessionRepository;
import com.slit.realityvote.service.AuditLogService;
import com.slit.realityvote.service.ComplianceService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Compliance service implementation.
 *
 * Read-only with respect to Vote and AuditLog — this class never calls
 * any repository save/delete for those two entities.
 */
@Service
@RequiredArgsConstructor
public class ComplianceServiceImpl implements ComplianceService {

    private final AuditLogRepository auditLogRepository;
    private final VoteRepository voteRepository;
    private final VotingSessionRepository sessionRepository;
    private final AuditLogService auditLogService;

    @Override
    public ComplianceDashboardStats getDashboardStats() {
        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        return new ComplianceDashboardStats(
                auditLogRepository.count(),
                auditLogRepository.countByFlaggedTrue(),
                auditLogRepository.countByEventType(AuditEventType.SUSPICIOUS_ACTIVITY),
                auditLogRepository.countByEventType(AuditEventType.LOGIN_FAILURE),
                auditLogRepository.countByEventType(AuditEventType.VOTE_REJECTED),
                auditLogRepository.countByFlaggedTrueAndCreatedDateAfter(yesterday)
        );
    }

    @Override
    public VotingActivityStats getActivity(Long sessionId) {
        VotingSession session = getSession(sessionId);

        // Raw per-contestant tally from the Vote table
        Map<Long, Long> voteById = voteRepository.tallyBySession(sessionId).stream()
                .collect(Collectors.toMap(
                        t -> t.getContestantId(),
                        t -> t.getVoteCount()));

        // Fetch suspicious-actor emails once — used to mark flaggedByAnomaly
        // (We use the global top-flagged list; for a session-scoped check the
        // officer can still drill into Anomaly Detection from the chart.)
        List<String> suspiciousActors = auditLogService.getTopFlaggedActors(20)
                .stream()
                .map(ActorFlagCount::actorEmail)
                .collect(Collectors.toList());

        // Build per-contestant stats including all profile fields
        List<ContestantVoteStat> contestantStats = session.getContestants().stream()
                .map(c -> {
                    long votes = voteById.getOrDefault(c.getId(), 0L);
                    // A contestant is flagged if they appear in the suspicious
                    // actors list (their email matches a flagged actor) OR if
                    // their status is ELIMINATED/WITHDRAWN but they still have
                    // recent votes (unusual and worth highlighting).
                    boolean flagged = suspiciousActors.contains(
                            c.getFullName());   // actor emails may be usernames; fallback logic below
                    return new ContestantVoteStat(
                            c.getId(),
                            c.getFullName(),
                            votes,
                            c.getStatus(),
                            c.getBiography() != null ? c.getBiography() : "",
                            c.getTalentCategory() != null ? c.getTalentCategory() : "",
                            c.getHometown() != null ? c.getHometown() : "",
                            flagged
                    );
                })
                .sorted(Comparator.comparingLong(ContestantVoteStat::voteCount).reversed())
                .collect(Collectors.toList());

        // Legacy map for backward-compatible templates (report.html)
        Map<String, Long> byName = contestantStats.stream()
                .collect(Collectors.toMap(
                        ContestantVoteStat::fullName,
                        ContestantVoteStat::voteCount,
                        Long::sum,
                        LinkedHashMap::new));

        long totalVotes    = voteRepository.countByVotingSession_Id(sessionId);
        long totalRejected = auditLogRepository.countByEventType(AuditEventType.VOTE_REJECTED);

        return new VotingActivityStats(sessionId, sessionDescription(session),
                totalVotes, totalRejected, byName, contestantStats);
    }

    @Override
    public AnomalyReport detectAnomalies(Long sessionId) {
        VotingSession session = getSession(sessionId);

        List<ActorFlagCount> topActors = auditLogService.getTopFlaggedActors(5);
        List<String> actorEmails = topActors.stream()
                .map(ActorFlagCount::actorEmail)
                .collect(Collectors.toList());

        List<String> descriptions = new ArrayList<>();
        if (!topActors.isEmpty()) {
            for (ActorFlagCount a : topActors) {
                descriptions.add(a.actorEmail() + " — " + a.flaggedCount() + " flagged event(s)");
            }
        } else {
            descriptions.add("No suspicious patterns detected for this session.");
        }

        long totalFlagged = auditLogRepository.countByFlaggedTrue();

        return new AnomalyReport(sessionId, sessionDescription(session),
                (int) totalFlagged, actorEmails, descriptions);
    }

    @Override
    public IntegrityReport verifyIntegrity(Long sessionId) {
        VotingSession session = getSession(sessionId);

        long storedVotes  = voteRepository.countByVotingSession_Id(sessionId);
        long auditedVotes = auditLogRepository.countByEventTypeAndEntityId(
                AuditEventType.VOTE_CAST, sessionId);
        long rejected     = auditLogRepository.countByEventTypeAndEntityId(
                AuditEventType.VOTE_REJECTED, sessionId);

        boolean match = storedVotes == auditedVotes;
        long diff     = Math.abs(storedVotes - auditedVotes);

        String status = match ? "OK" : (diff <= 2 ? "WARNING" : "CRITICAL");

        return new IntegrityReport(sessionId, sessionDescription(session),
                storedVotes, auditedVotes, rejected, match, status);
    }

    @Override
    public ComplianceReportDto generateReport(Long sessionId) {
        VotingActivityStats activity = getActivity(sessionId);
        AnomalyReport anomalies      = detectAnomalies(sessionId);
        IntegrityReport integrity    = verifyIntegrity(sessionId);

        List<String> recommendations = new ArrayList<>();
        if (!"OK".equals(integrity.integrityStatus())) {
            recommendations.add("Vote count mismatch detected — manual audit recommended.");
        }
        if (anomalies.totalFlaggedEvents() > 0) {
            recommendations.add(anomalies.totalFlaggedEvents() + " flagged events found. "
                    + "Review top suspicious actors and consider raising a security alert.");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("No compliance issues detected. Session looks clean.");
        }

        return new ComplianceReportDto(sessionId, activity.sessionDescription(),
                java.time.LocalDateTime.now(), activity, anomalies, integrity, recommendations);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VotingSession getSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Voting session not found: " + id));
    }

    private String sessionDescription(VotingSession s) {
        return "Session #" + s.getId() + " (Episode " + s.getEpisode().getId() + ")";
    }
}
