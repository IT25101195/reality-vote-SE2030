package com.slit.realityvote.repository;

import com.slit.realityvote.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeasonRepository extends JpaRepository<Season, Long> {
    List<Season> findByShowIdOrderBySeasonNumberAsc(Long showId);
}
