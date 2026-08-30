package com.slit.realityvote.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A Voting Session is created by an Administrator against one Episode,
 * for a chosen pool of Contestants, with a start/end window - directly
 * matching: "An administrator creates a voting session by selecting the
 * show, season, episode, contestants, and voting period, then activating
 * the session" (requirements doc, Voting Session Management).
 *
 * Multiple sessions can be OPEN at the same time for different
 * shows/episodes (requirement: "Yes, the system should support multiple
 * voting sessions running at the same time").
 */
@Entity
@Table(name = "voting_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VotingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @ManyToMany
    @JoinTable(
            name = "voting_session_contestants",
            joinColumns = @JoinColumn(name = "voting_session_id"),
            inverseJoinColumns = @JoinColumn(name = "contestant_id")
    )
    @Builder.Default
    private List<Contestant> contestants = new ArrayList<>();

    @NotNull(message = "Start time is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    @Builder.Default
    private VotingSessionStatus status = VotingSessionStatus.SCHEDULED;

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
