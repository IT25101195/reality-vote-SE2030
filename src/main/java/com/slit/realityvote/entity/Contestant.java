package com.slit.realityvote.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A Contestant participating in a Reality Show.
 *
 * A contestant is always assigned to a RealityShow, and optionally to a
 * specific Season (the requirements doc allows a contestant to be linked
 * to a show at the show level, with season/episode assignment "when
 * required" - so season is nullable here).
 *
 * Soft delete follows the same pattern as RealityShow: "Remove/Deactivate
 * Contestant" sets deleted=true rather than physically removing the row,
 * so a withdrawn contestant's historical votes/rankings are preserved.
 */
@Entity
@Table(name = "contestants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contestant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Full name is required")
    @Column(nullable = false)
    private String fullName;

    @NotNull(message = "Age is required")
    @Min(value = 15, message = "Contestant must be at least 15 years old")
    @Max(value = 100, message = "Enter a realistic age")
    private Integer age;

    @NotBlank(message = "Hometown is required")
    private String hometown;

    @NotBlank(message = "Talent category is required")
    private String talentCategory; // e.g. Singing, Dance, Comedy

    @NotBlank(message = "Biography is required")
    @Column(length = 2000)
    private String biography;

    /** Relative path under /uploads/contestants, e.g. "contestants/3_photo.jpg" */
    private String photoPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    @Builder.Default
    private ContestantStatus status = ContestantStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private RealityShow show;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id")
    private Season season; // optional - assigned "when required" per spec

    @Builder.Default
    private boolean deleted = false;

    // ---- Audit fields ----
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
}
