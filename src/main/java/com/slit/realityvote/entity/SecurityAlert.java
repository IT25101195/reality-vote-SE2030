package com.slit.realityvote.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A security alert raised by a Compliance Officer (PBI-15).
 *
 * Design constraint: No DELETE endpoint is ever exposed for this entity.
 * Once raised, an alert remains on record even after it is RESOLVED —
 * it is evidence and must not be erasable by the role that raised it.
 *
 * Status lifecycle: OPEN → INVESTIGATING → RESOLVED (one-way).
 */
@Entity
@Table(name = "security_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AlertSeverity severity = AlertSeverity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AlertStatus status = AlertStatus.OPEN;

    /** Email of the compliance officer who raised this alert. */
    @Column(nullable = false)
    private String raisedByEmail;

    /** Optionally links the alert to a specific VotingSession. */
    private Long sessionId;

    /** Optionally names the actor whose behaviour triggered the alert. */
    private String relatedActorEmail;

    /** Stamped when status transitions to RESOLVED. */
    private LocalDateTime resolvedAt;

    @Column(updatable = false)
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
        if (this.status == AlertStatus.RESOLVED && this.resolvedAt == null) {
            this.resolvedAt = LocalDateTime.now();
        }
    }
}
