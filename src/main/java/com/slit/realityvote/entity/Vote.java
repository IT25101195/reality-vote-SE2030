package com.slit.realityvote.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A single vote cast by a Viewer for one Contestant within one
 * VotingSession.
 *
 * Vote Integrity: the unique constraint below is a second, DB-enforced
 * line of defence behind the service-layer duplicate check in
 * VoteServiceImpl - even if two requests race each other, the database
 * itself rejects a second identical (session, contestant, voter) row.
 * This directly satisfies "One Vote Rules" / "Duplicate Vote Detection"
 * from the spec, not just as an application-level check.
 */
@Entity
@Table(name = "votes", uniqueConstraints = {
        @UniqueConstraint(name = "uq_one_vote_per_contestant_per_session",
                columnNames = {"voting_session_id", "contestant_id", "voter_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voting_session_id", nullable = false)
    private VotingSession votingSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contestant_id", nullable = false)
    private Contestant contestant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    private User voter;

    @Column(updatable = false)
    private LocalDateTime votedAt;

    @PrePersist
    protected void onCreate() {
        this.votedAt = LocalDateTime.now();
    }
}
