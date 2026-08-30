package com.slit.realityvote.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A complaint/support request submitted by a Viewer. The human-readable
 * reference (e.g. "TCK-000042") is what a user would quote when following
 * up - matches "a unique reference number" from the requirements doc.
 * Only Support Staff / Administrator can write staffResponse and change
 * status; the submitter can read but not edit their own ticket, which is
 * why there's no "update ticket" endpoint from the viewer's side.
 */
@Entity
@Table(name = "support_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Please describe the issue")
    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id", nullable = false)
    private User submittedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private User resolvedBy; // the support staff member who last responded

    @Column(length = 2000)
    private String staffResponse;

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
    }

    /** e.g. "TCK-000042" - only meaningful once the entity has an id (after save). */
    public String getReference() {
        return id == null ? "TCK-PENDING" : String.format("TCK-%06d", id);
    }
}
