package com.slit.realityvote.service.impl;

import com.slit.realityvote.dto.ActorFlagCount;
import com.slit.realityvote.dto.AuditSearchCriteria;
import com.slit.realityvote.entity.AuditEventType;
import com.slit.realityvote.entity.AuditLog;
import com.slit.realityvote.repository.AuditLogRepository;
import com.slit.realityvote.service.AuditLogService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fraud Detection (simple, explainable rule — good for a viva since you
 * can point at the exact threshold and reasoning, unlike a black-box ML
 * approach which nobody on the team could defend under questioning):
 *
 * If the same actor triggers WINDOW_LIMIT-or-more events of a *monitored*
 * type within WINDOW_MINUTES, the triggering event gets flagged=true and
 * a SUSPICIOUS_ACTIVITY row is written. Two patterns are monitored:
 *   - repeated VOTE_REJECTED  -> looks like someone probing vote rules
 *   - repeated LOGIN_FAILURE  -> looks like a brute-force login attempt
 * This matches the requirements doc: "monitor unusual patterns such as
 * repeated voting attempts... and repeated failed login attempts."
 */
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    private static final int WINDOW_MINUTES = 10;
    private static final long WINDOW_LIMIT = 3;

    private static final Set<AuditEventType> MONITORED_TYPES =
            Set.of(AuditEventType.VOTE_REJECTED, AuditEventType.LOGIN_FAILURE);

    // ── Existing ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void record(AuditEventType eventType, String description, String actorEmail) {
        record(eventType, description, actorEmail, null, null, null, null);
    }

    @Override
    @Transactional
    public void record(AuditEventType eventType, String description, String actorEmail,
                       String action, String entityType, Long entityId, String ipAddress) {
        String actor = (actorEmail == null || actorEmail.isBlank()) ? "anonymous" : actorEmail;

        boolean flagged = false;
        if (MONITORED_TYPES.contains(eventType)) {
            LocalDateTime since = LocalDateTime.now().minusMinutes(WINDOW_MINUTES);
            long recentCount = auditLogRepository.countByActorEmailAndEventTypeAndCreatedDateAfter(
                    actor, eventType, since);
            // +1 accounts for the event we're about to save
            flagged = (recentCount + 1) >= WINDOW_LIMIT;
        }

        auditLogRepository.save(AuditLog.builder()
                .eventType(eventType)
                .description(description)
                .actorEmail(actor)
                .flagged(flagged)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(ipAddress)
                .build());

        if (flagged) {
            auditLogRepository.save(AuditLog.builder()
                    .eventType(AuditEventType.SUSPICIOUS_ACTIVITY)
                    .description("Threshold reached: " + WINDOW_LIMIT + "+ " + eventType +
                            " events from '" + actor + "' within " + WINDOW_MINUTES + " minutes.")
                    .actorEmail(actor)
                    .flagged(true)
                    .ipAddress(ipAddress)
                    .build());
        }
    }

    @Override
    public Page<AuditLog> search(AuditEventType eventType, boolean flaggedOnly,
                                  String keyword, Pageable pageable) {
        String cleanKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return auditLogRepository.search(eventType, flaggedOnly, cleanKeyword, pageable);
    }

    @Override
    public long countFlagged() {
        return auditLogRepository.countByFlaggedTrue();
    }

    // ── Module 6.4 additions ─────────────────────────────────────────────────

    @Override
    public Optional<AuditLog> getById(Long id) {
        return auditLogRepository.findById(id);
    }

    @Override
    public List<AuditLog> getByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    @Override
    public void export(AuditSearchCriteria criteria, HttpServletResponse response) throws IOException {
        String keyword = (criteria.keyword() == null || criteria.keyword().isBlank())
                ? null : criteria.keyword().trim();
        List<AuditLog> logs = auditLogRepository.searchAll(
                criteria.eventType(), criteria.flaggedOnly(), keyword);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"audit-log-export.csv\"");

        try (PrintWriter writer = response.getWriter()) {
            writer.println("ID,Timestamp,EventType,Actor,Description,Flagged,Action,EntityType,EntityId,IPAddress");
            for (AuditLog log : logs) {
                writer.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        log.getId(),
                        log.getCreatedDate(),
                        log.getEventType(),
                        csv(log.getActorEmail()),
                        csv(log.getDescription()),
                        log.isFlagged(),
                        csv(log.getAction()),
                        csv(log.getEntityType()),
                        log.getEntityId() != null ? log.getEntityId() : "",
                        csv(log.getIpAddress()));
            }
        }
    }

    @Override
    @Transactional
    public int purge(LocalDateTime before) {
        return auditLogRepository.deleteByCreatedDateBeforeExcludingVoteEvents(before);
    }

    @Override
    public List<ActorFlagCount> getTopFlaggedActors(int limit) {
        return auditLogRepository.findTopFlaggedActors(PageRequest.of(0, limit))
                .stream()
                .map(p -> new ActorFlagCount(p.getActorEmail(), p.getFlaggedCount()))
                .collect(Collectors.toList());
    }

    @Override
    public long countByEventType(AuditEventType eventType) {
        return auditLogRepository.countByEventType(eventType);
    }

    @Override
    public long countFlaggedSince(LocalDateTime since) {
        return auditLogRepository.countByFlaggedTrueAndCreatedDateAfter(since);
    }

    private static String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"")) return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }
}
