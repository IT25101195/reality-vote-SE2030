package com.slit.realityvote.service;

import com.slit.realityvote.dto.ActorFlagCount;
import com.slit.realityvote.entity.AuditEventType;
import com.slit.realityvote.entity.AuditLog;
import com.slit.realityvote.repository.AuditLogRepository;
import com.slit.realityvote.service.impl.AuditLogServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditLogServiceImpl (Module 6.4).
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock AuditLogRepository auditLogRepository;

    @InjectMocks
    AuditLogServiceImpl auditLogService;

    @Test
    void record_basicOverload_savesLogWithNullOptionalFields() {
        auditLogService.record(AuditEventType.LOGIN_SUCCESS, "Logged in", "user@example.com");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertThat(saved.getEventType()).isEqualTo(AuditEventType.LOGIN_SUCCESS);
        assertThat(saved.getActorEmail()).isEqualTo("user@example.com");
        assertThat(saved.isFlagged()).isFalse();
        assertThat(saved.getIpAddress()).isNull();
    }

    @Test
    void record_extendedOverload_savesAllFields() {
        auditLogService.record(AuditEventType.VOTE_CAST, "Vote cast", "voter@test.com",
                "CAST_VOTE", "Vote", 42L, "192.168.1.1");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertThat(saved.getAction()).isEqualTo("CAST_VOTE");
        assertThat(saved.getEntityType()).isEqualTo("Vote");
        assertThat(saved.getEntityId()).isEqualTo(42L);
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    void record_repeatedLoginFailures_flagsAndWritesSuspiciousActivity() {
        // Simulate WINDOW_LIMIT - 1 = 2 existing failures for this actor
        when(auditLogRepository.countByActorEmailAndEventTypeAndCreatedDateAfter(
                eq("bad@test.com"), eq(AuditEventType.LOGIN_FAILURE), any()))
                .thenReturn(2L);   // 2 + 1 (new) = 3 >= WINDOW_LIMIT

        auditLogService.record(AuditEventType.LOGIN_FAILURE, "Bad password", "bad@test.com");

        // Should save 2 rows: the LOGIN_FAILURE itself + a SUSPICIOUS_ACTIVITY row
        verify(auditLogRepository, times(2)).save(any(AuditLog.class));
    }

    @Test
    void getById_delegatesToRepository() {
        AuditLog log = AuditLog.builder().id(7L).build();
        when(auditLogRepository.findById(7L)).thenReturn(Optional.of(log));

        Optional<AuditLog> result = auditLogService.getById(7L);

        assertThat(result).isPresent().contains(log);
    }

    @Test
    void purge_returnsDeletedCount() {
        when(auditLogRepository.deleteByCreatedDateBeforeExcludingVoteEvents(any())).thenReturn(42);
        assertThat(auditLogService.purge(LocalDateTime.now().minusDays(365))).isEqualTo(42);
    }

    @Test
    void countByEventType_delegatesToRepository() {
        when(auditLogRepository.countByEventType(AuditEventType.VOTE_REJECTED)).thenReturn(15L);
        assertThat(auditLogService.countByEventType(AuditEventType.VOTE_REJECTED)).isEqualTo(15L);
    }
}
