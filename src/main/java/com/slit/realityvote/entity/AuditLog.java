package com.slit.realityvote.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * An append-only audit trail row. Deliberately has no update/delete path
 * anywhere in the codebase - "the system shall ensure that votes cannot
 * be altered after submission" extends here too: once an event is
 * logged, it stays logged, satisfying "Vote Integrity" / "Auditability"
 * (retain for 5 years, per the requirements doc).
 *
 * `flagged` marks rows raised by the simple fraud-detection rule in
 * AuditLogServiceImpl (repeated rejected votes / failed logins in a
 * short window) so the Compliance Officer's dashboard can surface them
 * without scanning the whole table.
 *
 * Module 6.4 additions (nullable, backward-compatible):
 *   action     — free-text action label used by AuditLoggingAspect
 *   entityType — which domain object was affected (e.g. "Vote", "VotingSession")
 *   entityId   — the PK of that object
 *   ipAddress  — remote address captured from the HTTP request
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditEventType eventType;

    @Column(nullable = false, length = 500)
    private String description;

    /** Email of the user who triggered the event, or "anonymous" for pre-login attempts. */
    @Column(nullable = false)
    private String actorEmail;

    @Builder.Default
    private boolean flagged = false;

    @Column(updatable = false)
    private LocalDateTime createdDate;

    // ── Module 6.4 additions ──────────────────────────────────────────────────

    /** Free-text action label set by AuditLoggingAspect (e.g. "CAST_VOTE", "OPEN_SESSION"). */
    @Column(length = 100)
    private String action;

    /** Domain type of the affected entity, e.g. "Vote", "VotingSession", "User". */
    @Column(length = 100)
    private String entityType;

    /** Primary key of the affected entity; null for events not tied to a single record. */
    private Long entityId;

    /** Remote IP address captured from the HTTP request; may be null for internal/scheduled events. */
    @Column(length = 60)
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
    }
}
