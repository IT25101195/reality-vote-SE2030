package com.slit.realityvote.repository;

import com.slit.realityvote.entity.Contestant;
import com.slit.realityvote.entity.ContestantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContestantRepository extends JpaRepository<Contestant, Long> {

    Optional<Contestant> findByIdAndDeletedFalse(Long id);

    // Duplicate-detection: same name already registered for the same show
    // (requirements doc: "the system should automatically detect and
    // prevent duplicate contestant records").
    boolean existsByFullNameIgnoreCaseAndShow_IdAndDeletedFalse(String fullName, Long showId);

    /**
     * Combined search + filter + pagination in one query. Any of keyword /
     * showId / status can be null, in which case that filter is skipped -
     * avoids building a separate query method for every combination.
     */
    @Query("SELECT c FROM Contestant c WHERE c.deleted = false " +
           "AND (:keyword IS NULL OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:showId IS NULL OR c.show.id = :showId) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "ORDER BY c.fullName ASC")
    Page<Contestant> search(@Param("keyword") String keyword,
                             @Param("showId") Long showId,
                             @Param("status") ContestantStatus status,
                             Pageable pageable);
}
