package com.slit.realityvote.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A Reality Show (e.g. "Sri Lanka Star Season 5").
 * A show has many Seasons; each Season has many Episodes.
 *
 * Relationships:
 *   RealityShow (1) --- (many) Season (1) --- (many) Episode
 *
 * Soft delete: instead of physically removing a row (which would break
 * historical voting records tied to it), "Delete Show" sets deleted=true
 * and status=ARCHIVED. Queries use RealityShowRepository.findByDeletedFalse()
 * so archived shows disappear from normal listings but the data survives
 * for reports.
 */
@Entity
@Table(name = "reality_shows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealityShow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Show name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Description is required")
    @Column(nullable = false, length = 1000)
    private String description;

    @NotBlank(message = "Category is required")
    private String category; // e.g. Singing, Dance, Talent

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30)")
    @Builder.Default
    private ShowStatus status = ShowStatus.UPCOMING;

    /** Relative path (under /uploads) to the show's poster/cover photo, e.g. "shows/abc123.jpg". Nullable. */
    private String posterImagePath;

    @Builder.Default
    private boolean deleted = false; // soft delete flag

    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Season> seasons = new ArrayList<>();

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

    /** Convenience method to keep both sides of the relationship in sync. */
    public void addSeason(Season season) {
        seasons.add(season);
        season.setShow(this);
    }
}
