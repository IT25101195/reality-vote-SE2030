package com.slit.realityvote.service.impl;

import com.slit.realityvote.entity.Episode;
import com.slit.realityvote.entity.RealityShow;
import com.slit.realityvote.entity.Season;
import com.slit.realityvote.entity.ShowStatus;
import com.slit.realityvote.repository.RealityShowRepository;
import com.slit.realityvote.repository.SeasonRepository;
import com.slit.realityvote.service.RealityShowService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor // Lombok generates a constructor for the final fields below (constructor injection)
public class RealityShowServiceImpl implements RealityShowService {

    private final RealityShowRepository showRepository;
    private final SeasonRepository seasonRepository;

    @Override
    public List<RealityShow> getAllActiveShows() {
        return showRepository.findByDeletedFalse();
    }

    @Override
    public RealityShow getShowById(Long id) {
        return showRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Reality show not found with id: " + id));
    }

    @Override
    @Transactional
    public RealityShow createShow(RealityShow show) {
        // Business rule: end date must not be before start date
        if (show.getEndDate().isBefore(show.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        show.setStatus(ShowStatus.UPCOMING);
        show.setDeleted(false);
        return showRepository.save(show);
    }

    @Override
    @Transactional
    public RealityShow updateShow(Long id, RealityShow updatedShow) {
        RealityShow existing = getShowById(id);

        if (updatedShow.getEndDate().isBefore(updatedShow.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        existing.setName(updatedShow.getName());
        existing.setDescription(updatedShow.getDescription());
        existing.setCategory(updatedShow.getCategory());
        existing.setStartDate(updatedShow.getStartDate());
        existing.setEndDate(updatedShow.getEndDate());
        existing.setStatus(updatedShow.getStatus());
        if (updatedShow.getPosterImagePath() != null) {
            existing.setPosterImagePath(updatedShow.getPosterImagePath());
        }
        // createdDate/updatedDate handled automatically by @PreUpdate

        return showRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteShow(Long id) {
        RealityShow show = getShowById(id);
        // Soft delete: we archive rather than physically remove, so that
        // historical voting/report data referencing this show is preserved.
        show.setDeleted(true);
        show.setStatus(ShowStatus.ARCHIVED);
        showRepository.save(show);
    }

    @Override
    public List<RealityShow> searchShows(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllActiveShows();
        }
        return showRepository.searchByName(keyword.trim());
    }

    @Override
    @Transactional
    public Season addSeasonToShow(Long showId, Season season) {
        RealityShow show = getShowById(showId);
        show.addSeason(season); // keeps both sides of the relationship in sync
        // Don't also call seasonRepository.save(season) here - RealityShow.seasons
        // is cascade = CascadeType.ALL, so saving the show already persists the
        // new season. Calling save() on both sides at once was the cause of
        // "detached entity passed to persist": Hibernate got two competing
        // instructions for the same not-yet-persisted Season.
        showRepository.save(show);
        return season;
    }

    @Override
    @Transactional
    public Episode addEpisodeToSeason(Long seasonId, Episode episode) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new EntityNotFoundException("Season not found with id: " + seasonId));
        season.addEpisode(episode); // keeps both sides of the relationship in sync
        // Same fix as above: Season.episodes is cascade = CascadeType.ALL,
        // so saving the season cascades the new episode - no separate
        // episodeRepository.save(episode) call needed (or wanted).
        seasonRepository.save(season);
        return episode;
    }
}
