package com.slit.realityvote.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Persistent snapshot of a compliance report for one VotingSession.
 *
 * Generated on demand from ComplianceService.generateReport(), then saved
 * so the officer can reference the same report later without re-running
 * live calculations against possibly-changed data.
 *
 * No delete endpoint — a filed report is an official record.
 */
@Entity
@Table(name = "compliance_report_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceReportRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sessionId;
    private String sessionDescription;
    private LocalDateTime generatedAt;

    /** OK / WARNING / CRITICAL from IntegrityReport */
    private String integrityStatus;
    private long totalVotes;
    private long totalFlagged;
    private long totalRejected;

    /** Newline-delimited recommendations from the report. */
    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private ReportStatus status = ReportStatus.DRAFT;

    private LocalDateTime filedAt;

    @Column(nullable = false)
    private String raisedByEmail;

    @Column(updatable = false)
    private LocalDateTime createdDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
}
