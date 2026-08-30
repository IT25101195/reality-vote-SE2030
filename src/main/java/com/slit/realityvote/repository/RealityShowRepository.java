package com.slit.realityvote.repository;

import com.slit.realityvote.entity.RealityShow;
import com.slit.realityvote.entity.ShowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository Pattern:
 * This interface is our data-access abstraction. The controller/service
 * layers never write SQL or talk to Hibernate directly - they call methods
 * here, and Spring Data JPA generates the implementation at runtime from
 * the method names / @Query annotations. This decouples business logic
 * from persistence details (easy to explain in the viva: "we never touch
 * the database directly outside this layer").
 */
public interface RealityShowRepository extends JpaRepository<RealityShow, Long> {

    // Active (non soft-deleted) shows, for normal listings
    List<RealityShow> findByDeletedFalse();

    // Single active show by id - used when editing/viewing
    Optional<RealityShow> findByIdAndDeletedFalse(Long id);

    List<RealityShow> findByDeletedFalseAndStatus(ShowStatus status);

    // Simple case-insensitive search by name, for the search bar
    @Query("SELECT s FROM RealityShow s WHERE s.deleted = false " +
           "AND LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<RealityShow> searchByName(@Param("keyword") String keyword);
}
