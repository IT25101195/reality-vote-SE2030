package com.slit.realityvote.service;

import com.slit.realityvote.entity.RealityShow;
import com.slit.realityvote.entity.Season;

import java.util.List;

/**
 * Service layer contract. The controller depends on this interface, not
 * the implementation class - so the implementation could be swapped
 * (e.g. for a test double) without touching controller code. This is
 * standard layered-architecture practice from the module: Controller ->
 * Service -> Repository.
 */
public interface RealityShowService {

    List<RealityShow> getAllActiveShows();

    RealityShow getShowById(Long id);

    RealityShow createShow(RealityShow show);

    RealityShow updateShow(Long id, RealityShow updatedShow);

    void deleteShow(Long id); // soft delete -> archives the show

    List<RealityShow> searchShows(String keyword);

    Season addSeasonToShow(Long showId, Season season);

    com.slit.realityvote.entity.Episode addEpisodeToSeason(Long seasonId, com.slit.realityvote.entity.Episode episode);
}
