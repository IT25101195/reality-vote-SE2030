package com.slit.realityvote.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A persisted snapshot of fraud/anomaly findings for one VotingSession.
 *
 * Created by snapshotting the live ComplianceService.detectAnomalies() output
 * at the moment the officer presses "Save Report".  Can be revised (DRAFT)
 * and then formally filed (FILED → UNDER_REVIEW → RESOLVED), but never deleted.
 */
@Entity
@Table(name = "fraud_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sessionId;
    private String sessionDescription;

    /** Officer's narrative — editable while status is DRAFT. */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /** Newline-delimited list of detected pattern descriptions. */
    @Column(columnDefinition = "TEXT")
    private String detectedPatterns;

    /** Comma-delimited list of suspicious actor emails. */
    @Column(columnDefinition = "TEXT")
    private String suspiciousActors;

    private int totalFlaggedEvents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FraudReportStatus status = FraudReportStatus.DRAFT;

    @Column(nullable = false)
    private String raisedByEmail;

    @Column(updatable = false)
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        updatedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
