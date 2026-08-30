package com.slit.realityvote.repository;

import com.slit.realityvote.entity.AuditEventType;
import com.slit.realityvote.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // ── Existing search (unchanged) ──────────────────────────────────────────

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:eventType IS NULL OR a.eventType = :eventType) " +
           "AND (:flaggedOnly = false OR a.flagged = true) " +
           "AND (:keyword IS NULL OR LOWER(a.actorEmail) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY a.createdDate DESC")
    Page<AuditLog> search(@Param("eventType") AuditEventType eventType,
                           @Param("flaggedOnly") boolean flaggedOnly,
                           @Param("keyword") String keyword,
                           Pageable pageable);

    // Used by the fraud-detection rule: how many matching events has this
    // actor triggered since `since`? (e.g. repeated VOTE_REJECTED or
    // LOGIN_FAILURE in the last few minutes)
    long countByActorEmailAndEventTypeAndCreatedDateAfter(String actorEmail, AuditEventType eventType, LocalDateTime since);

    long countByFlaggedTrue();

    // ── Module 6.4 additions ─────────────────────────────────────────────────

    /**
     * AuditLogRepository#findByActorEmailAndCreatedDateBetween — used by the
     * compliance search for per-actor time-range queries (Module 6.4).
     */
    List<AuditLog> findByActorEmailAndCreatedDateBetween(String actorEmail,
                                                          LocalDateTime from,
                                                          LocalDateTime to);

    /**
     * All audit entries for a specific domain entity — used by
     * GET /compliance/audit-logs/entity/{type}/{entityId}.
     */
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    /**
     * All logs of a specific event type — used by event-breakdown statistics.
     */
    List<AuditLog> findByEventType(AuditEventType eventType);

    /**
     * Purge old audit entries. Hard delete; intentionally an admin-only
     * operation that requires ADMINISTRATOR authority (see SecurityConfig).
     * AuditLogs for VOTE_CAST or VOTE_REJECTED are deliberately excluded to
     * preserve the immutable vote-integrity trail.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM AuditLog a WHERE a.createdDate < :before " +
           "AND a.eventType NOT IN ('VOTE_CAST', 'VOTE_REJECTED')")
    int deleteByCreatedDateBeforeExcludingVoteEvents(@Param("before") LocalDateTime before);

    /**
     * How many events of a given entity type occurred in a time window.
     * Backs the compliance activity heatmap.
     */
    long countByEntityTypeAndCreatedDateBetween(String entityType,
                                                 LocalDateTime from,
                                                 LocalDateTime to);

    /** KPI: events of exactly this type. */
    long countByEventType(AuditEventType eventType);

    /** How many flagged events were raised in the last N hours/days. */
    long countByFlaggedTrueAndCreatedDateAfter(LocalDateTime since);

    /**
     * Export-friendly list (no pagination) matching the same filters as
     * the paginated search. Used by CSV export endpoint.
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:eventType IS NULL OR a.eventType = :eventType) " +
           "AND (:flaggedOnly = false OR a.flagged = true) " +
           "AND (:keyword IS NULL OR LOWER(a.actorEmail) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY a.createdDate DESC")
    List<AuditLog> searchAll(@Param("eventType") AuditEventType eventType,
                              @Param("flaggedOnly") boolean flaggedOnly,
                              @Param("keyword") String keyword);

    /**
     * Top flagged actors: actor email + count. Used by the suspicious-actors
     * widget on the compliance dashboard.
     */
    @Query("SELECT a.actorEmail AS actorEmail, COUNT(a) AS flaggedCount " +
           "FROM AuditLog a WHERE a.flagged = true " +
           "GROUP BY a.actorEmail ORDER BY COUNT(a) DESC")
    List<ActorProjection> findTopFlaggedActors(Pageable pageable);

    /**
     * Count VOTE_CAST events for a specific voting session, using the
     * entityId field set by AuditLoggingAspect on every cast.
     */
    long countByEventTypeAndEntityId(AuditEventType eventType, Long entityId);

    /**
     * Count VOTE_REJECTED events for a specific voting session.
     * Identical pattern to the above — two separate calls in the service
     * keep the JPQL simple.
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.eventType = :eventType AND a.entityId = :entityId")
    long countVoteEventsBySession(@Param("eventType") AuditEventType eventType,
                                   @Param("entityId") Long entityId);

    // Nested projection for the top-flagged-actors query above
    interface ActorProjection {
        String getActorEmail();
        long getFlaggedCount();
    }
}
