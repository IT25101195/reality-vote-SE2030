package com.slit.realityvote.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

/**
 * A single Episode within a Season. votingOpen controls whether viewers
 * can currently cast votes tied to this episode (the Voting module,
 * built separately, will read/toggle this flag).
 */
@Entity
@Table(name = "episodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Episode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Episode title is required")
    private String title;

    @NotNull(message = "Episode number is required")
    private Integer episodeNumber;

    private LocalDate airDate;

    @Builder.Default
    private boolean votingOpen = false;

    /**
     * Mirrors votingOpen but controls the judges' "designated judging
     * period" instead (see JudgeAssignment / Score). Kept as a separate
     * flag because judging and audience voting windows don't have to
     * line up - a panel may score a live episode while public voting
     * opens later, or vice versa.
     */
    @Builder.Default
    private boolean judgingOpen = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;
}
