package com.slit.realityvote.service.impl;

import com.slit.realityvote.dto.VoteTallyView;
import com.slit.realityvote.entity.*;
import com.slit.realityvote.repository.ContestantRepository;
import com.slit.realityvote.repository.UserRepository;
import com.slit.realityvote.repository.VoteRepository;
import com.slit.realityvote.repository.VotingSessionRepository;
import com.slit.realityvote.service.AuditLogService;
import com.slit.realityvote.service.VoteService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteServiceImpl implements VoteService {

    private final VoteRepository voteRepository;
    private final VotingSessionRepository sessionRepository;
    private final ContestantRepository contestantRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public Vote castVote(Long sessionId, Long contestantId, Long voterId) {
        VotingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Voting session not found with id: " + sessionId));

        User voterForLog = userRepository.findById(voterId).orElse(null);
        String actorEmail = voterForLog != null ? voterForLog.getEmail() : "unknown-voter-" + voterId;

        // Rule 1: the session must actually be open right now.
        if (session.getStatus() != VotingSessionStatus.OPEN) {
            auditLogService.record(AuditEventType.VOTE_REJECTED,
                    "Vote rejected: session " + sessionId + " is not OPEN (status=" + session.getStatus() + ")",
                    actorEmail);
            throw new IllegalStateException("This voting session is not currently open for voting.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(session.getStartTime()) || now.isAfter(session.getEndTime())) {
            auditLogService.record(AuditEventType.VOTE_REJECTED,
                    "Vote rejected: outside voting window for session " + sessionId, actorEmail);
            throw new IllegalStateException("This voting session is outside its scheduled voting window.");
        }

        Contestant contestant = contestantRepository.findByIdAndDeletedFalse(contestantId)
                .orElseThrow(() -> new EntityNotFoundException("Contestant not found with id: " + contestantId));

        // Rule 2: the contestant must actually belong to this session's pool
        // - stops someone voting for a contestant who isn't even in this
        // episode's voting round.
        boolean eligible = session.getContestants().stream().anyMatch(c -> c.getId().equals(contestantId));
        if (!eligible) {
            auditLogService.record(AuditEventType.VOTE_REJECTED,
                    "Vote rejected: contestant " + contestantId + " not eligible for session " + sessionId, actorEmail);
            throw new IllegalStateException("This contestant is not part of the selected voting session.");
        }

        User voter = userRepository.findById(voterId)
                .orElseThrow(() -> new EntityNotFoundException("Voter account not found."));

        // Rule 3: one vote per contestant per session per viewer
        // (application-level check; the DB unique constraint on Vote is
        // the second, authoritative line of defence against race conditions).
        if (voteRepository.existsByVotingSession_IdAndContestant_IdAndVoter_Id(sessionId, contestantId, voterId)) {
            auditLogService.record(AuditEventType.VOTE_REJECTED,
                    "Vote rejected: duplicate vote attempt for contestant " + contestantId + " in session " + sessionId,
                    actorEmail);
            throw new IllegalStateException("You have already voted for this contestant in this session.");
        }

        Vote vote = Vote.builder()
                .votingSession(session)
                .contestant(contestant)
                .voter(voter)
                .build();

        Vote saved = voteRepository.save(vote);
        auditLogService.record(AuditEventType.VOTE_CAST,
                "Vote cast for contestant " + contestantId + " in session " + sessionId, actorEmail);
        return saved;
    }

    @Override
    public List<VoteTallyView> getLiveResults(Long sessionId) {
        VotingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Voting session not found with id: " + sessionId));

        long totalVotes = voteRepository.countByVotingSession_Id(sessionId);

        Map<Long, Long> tallyByContestantId = voteRepository.tallyBySession(sessionId).stream()
                .collect(Collectors.toMap(
                        VoteRepository.ContestantTally::getContestantId,
                        VoteRepository.ContestantTally::getVoteCount));

        // Every contestant in the session pool appears, even with 0 votes -
        // otherwise a contestant with no votes yet would be missing from
        // the leaderboard entirely.
        return session.getContestants().stream()
                .map(c -> {
                    long votes = tallyByContestantId.getOrDefault(c.getId(), 0L);
                    double pct = totalVotes == 0 ? 0.0 : (votes * 100.0 / totalVotes);
                    return new VoteTallyView(c.getId(), c.getFullName(), c.getPhotoPath(), votes,
                            Math.round(pct * 10.0) / 10.0);
                })
                .sorted(Comparator.comparingLong(VoteTallyView::voteCount).reversed())
                .toList();
    }

    @Override
    public List<Vote> getVotingHistoryForVoter(Long voterId) {
        return voteRepository.findByVoter_IdOrderByVotedAtDesc(voterId);
    }
}
