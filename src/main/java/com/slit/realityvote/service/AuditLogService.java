package com.slit.realityvote.service;

import com.slit.realityvote.dto.ActorFlagCount;
import com.slit.realityvote.dto.AuditSearchCriteria;
import com.slit.realityvote.entity.AuditEventType;
import com.slit.realityvote.entity.AuditLog;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuditLogService {

    // ── Existing ─────────────────────────────────────────────────────────────

    /** Records an event and runs the suspicious-pattern check for that actor. */
    void record(AuditEventType eventType, String description, String actorEmail);

    Page<AuditLog> search(AuditEventType eventType, boolean flaggedOnly, String keyword, Pageable pageable);

    long countFlagged();

    // ── Module 6.4 additions ─────────────────────────────────────────────────

    /**
     * Extended record: includes entityType, entityId and ipAddress fields
     * set by the AuditLoggingAspect for richer compliance queries.
     */
    void record(AuditEventType eventType, String description, String actorEmail,
                String action, String entityType, Long entityId, String ipAddress);

    Optional<AuditLog> getById(Long id);

    /** Fetch all audit entries for a specific domain entity (used by detail pages). */
    List<AuditLog> getByEntity(String entityType, Long entityId);

    /**
     * Export filtered results to a CSV file written directly to the response
     * OutputStream. No library dependency — plain CSV, matching the project's
     * existing CSV export pattern in ReportsController.
     */
    void export(AuditSearchCriteria criteria, HttpServletResponse response) throws IOException;

    /**
     * Delete audit entries older than {@code before} that are NOT vote events.
     * Only ADMINISTRATOR may call this; vote-related rows are never purged.
     * @return number of rows deleted
     */
    int purge(LocalDateTime before);

    /** Top actors by flagged event count, for the suspicious-actors dashboard widget. */
    List<ActorFlagCount> getTopFlaggedActors(int limit);

    /** Event count for one specific type (used in KPI cards). */
    long countByEventType(AuditEventType eventType);

    /** Flagged events raised since the given timestamp. */
    long countFlaggedSince(LocalDateTime since);
}
