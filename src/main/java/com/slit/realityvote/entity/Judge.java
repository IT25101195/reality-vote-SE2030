package com.slit.realityvote.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A Judge who evaluates contestants and submits scores.
 *
 * A Judge is a profile record managed by the Administrator (Judge & Panel
 * Management module) AND, separately, a login account with Role.JUDGE
 * (see User / DatabaseUserDetailsService). The two are linked loosely by
 * email rather than a foreign key, the same way the rest of the system
 * keeps "who is logged in" (User) separate from "the domain profile"
 * (Contestant, etc.) - ScoreServiceImpl looks up the Judge profile that
 * matches the authenticated principal's email.
 *
 * Soft delete follows the same pattern as Contestant/RealityShow: a
 * withdrawn judge's historical scores must survive for reports, so
 * "Remove/Deactivate Judge" sets deleted=true instead of removing the row.
 */
@Entity
@Table(name = "judges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Judge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Full name is required")
    @Column(nullable = false)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Column(nullable = false, unique = true)
    private String email; // matched against the logged-in JUDGE user's email

    private String phone;

    @NotBlank(message = "Expertise area is required")
    private String expertiseArea; // e.g. Vocal Coach, Choreographer, Music Producer

    @Column(length = 1000)
    private String bio;

    /** Relative path under /uploads/judges, e.g. "judges/3_photo.jpg" */
    private String photoPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    @Builder.Default
    private JudgeStatus status = JudgeStatus.ACTIVE;

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