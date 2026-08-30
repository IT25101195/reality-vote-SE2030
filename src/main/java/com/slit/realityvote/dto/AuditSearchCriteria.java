package com.slit.realityvote.dto;

import com.slit.realityvote.entity.AuditEventType;
import java.time.LocalDateTime;

/**
 * Filter parameters for audit log search and CSV/JSON export operations.
 */
public record AuditSearchCriteria(
        AuditEventType eventType,
        boolean flaggedOnly,
        String keyword,
        String entityType,
        Long entityId,
        LocalDateTime from,
        LocalDateTime to
) {}
