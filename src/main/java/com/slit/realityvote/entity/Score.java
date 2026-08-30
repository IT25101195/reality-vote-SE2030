package com.slit.realityvote.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One Judge's score for one Contestant in one Episode.
 *
 * A judge may only have ONE score row per (judge, contestant, episode) -
 * "Submit Contestant Scores" creates it, "Update/Revise Scores" edits the
 * same row (see the unique constraint), which is how ScoreServiceImpl
 * tells a first submission from a revision and keeps "View Scoring
 * History" free of duplicate rows for the same performance.
 *
 * Revision is only allowed while the episode's judging window is open
 * (Episode.judgingOpen) - the "within allowed time window" requirement.
 */
@Entity
@Table(name = "scores", uniqueConstraints = {
        @UniqueConstraint(name = "uq_one_score_per_judge_contestant_episode",
                columnNames = {"judge_id", "contestant_id", "episode_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "judge_id", nullable = false)
    private Judge judge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contestant_id", nullable = false)
    private Contestant contestant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @NotNull(message = "Score is required")
    @Min(value = 0, message = "Score must be between 0 and 100")
    @Max(value = 100, message = "Score must be between 0 and 100")
    private Integer scoreValue;

    @Column(length = 1000)
    private String remarks;

    @Column(updatable = false)
    private LocalDateTime submittedDate;
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        this.submittedDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}
