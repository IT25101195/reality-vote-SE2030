package com.slit.realityvote.service;

import com.slit.realityvote.dto.ComplianceDashboardStats;
import com.slit.realityvote.dto.IntegrityReport;
import com.slit.realityvote.entity.AuditEventType;
import com.slit.realityvote.entity.VotingSession;
import com.slit.realityvote.entity.Episode;
import com.slit.realityvote.repository.AuditLogRepository;
import com.slit.realityvote.repository.VoteRepository;
import com.slit.realityvote.repository.VotingSessionRepository;
import com.slit.realityvote.service.impl.ComplianceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ComplianceServiceImpl (Module 6.4).
 * Uses Mockito to isolate the service from its repositories.
 */
@ExtendWith(MockitoExtension.class)
class ComplianceServiceTest {

    @Mock AuditLogRepository auditLogRepository;
    @Mock VoteRepository voteRepository;
    @Mock VotingSessionRepository sessionRepository;
    @Mock AuditLogService auditLogService;

    @InjectMocks
    ComplianceServiceImpl complianceService;

    @Test
    void getDashboardStats_returnsAggregatedKpis() {
        when(auditLogRepository.count()).thenReturn(500L);
        when(auditLogRepository.countByFlaggedTrue()).thenReturn(12L);
        when(auditLogRepository.countByEventType(AuditEventType.SUSPICIOUS_ACTIVITY)).thenReturn(3L);
        when(auditLogRepository.countByEventType(AuditEventType.LOGIN_FAILURE)).thenReturn(45L);
        when(auditLogRepository.countByEventType(AuditEventType.VOTE_REJECTED)).thenReturn(8L);
        when(auditLogRepository.countByFlaggedTrueAndCreatedDateAfter(any())).thenReturn(2L);

        ComplianceDashboardStats stats = complianceService.getDashboardStats();

        assertThat(stats.totalEvents()).isEqualTo(500L);
        assertThat(stats.flaggedEvents()).isEqualTo(12L);
        assertThat(stats.suspiciousActivityCount()).isEqualTo(3L);
        assertThat(stats.loginFailures()).isEqualTo(45L);
        assertThat(stats.voteRejections()).isEqualTo(8L);
        assertThat(stats.flaggedLast24h()).isEqualTo(2L);
    }

    @Test
    void verifyIntegrity_countsMatch_returnsOkStatus() {
        Episode episode = new Episode();
        episode.setId(1L);
        VotingSession session = VotingSession.builder()
                .id(99L)
                .episode(episode)
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().plusHours(1))
                .contestants(List.of())
                .build();

        when(sessionRepository.findById(99L)).thenReturn(Optional.of(session));
        when(voteRepository.countByVotingSession_Id(99L)).thenReturn(100L);
        when(auditLogRepository.countByEventTypeAndEntityId(AuditEventType.VOTE_CAST, 99L)).thenReturn(100L);
        when(auditLogRepository.countByEventTypeAndEntityId(AuditEventType.VOTE_REJECTED, 99L)).thenReturn(5L);

        IntegrityReport report = complianceService.verifyIntegrity(99L);

        assertThat(report.countsMatch()).isTrue();
        assertThat(report.integrityStatus()).isEqualTo("OK");
        assertThat(report.storedVoteCount()).isEqualTo(100L);
        assertThat(report.auditedVoteCount()).isEqualTo(100L);
        assertThat(report.rejectedVoteCount()).isEqualTo(5L);
    }

    @Test
    void verifyIntegrity_countsMismatch_returnsCriticalStatus() {
        Episode episode = new Episode();
        episode.setId(1L);
        VotingSession session = VotingSession.builder()
                .id(77L)
                .episode(episode)
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().plusHours(1))
                .contestants(List.of())
                .build();

        when(sessionRepository.findById(77L)).thenReturn(Optional.of(session));
        when(voteRepository.countByVotingSession_Id(77L)).thenReturn(200L);
        when(auditLogRepository.countByEventTypeAndEntityId(AuditEventType.VOTE_CAST, 77L)).thenReturn(190L);
        when(auditLogRepository.countByEventTypeAndEntityId(AuditEventType.VOTE_REJECTED, 77L)).thenReturn(0L);

        IntegrityReport report = complianceService.verifyIntegrity(77L);

        assertThat(report.countsMatch()).isFalse();
        assertThat(report.integrityStatus()).isEqualTo("CRITICAL");
    }
}
