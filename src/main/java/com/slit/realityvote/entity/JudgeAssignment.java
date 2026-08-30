package com.slit.realityvote.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Assigns one Judge onto a judging panel scoped to a RealityShow and,
 * optionally, a specific Season and/or Episode - matching "the system
 * must let administrators assign judges to shows, seasons, episodes,
 * and judging panels" from the requirements doc.
 *
 * panelName groups several JudgeAssignment rows into one named panel
 * (e.g. "Grand Finale Panel"): several judges can share the same
 * panelName for the same show/season/episode. Leaving season/episode
 * null means the judge is assigned at the show level (all episodes).
 */
@Entity
@Table(name = "judge_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JudgeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Judge is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "judge_id", nullable = false)
    private Judge judge;

    @NotNull(message = "Reality show is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private RealityShow show;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id")
    private Season season; // optional - null = whole show

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id")
    private Episode episode; // optional - null = whole season/show

    @NotBlank(message = "Panel name is required")
    private String panelName; // e.g. "Semi-Final Judging Panel"

    @Column(updatable = false)
    private LocalDateTime assignedDate;

    @PrePersist
    protected void onCreate() {
        this.assignedDate = LocalDateTime.now();
    }
}
