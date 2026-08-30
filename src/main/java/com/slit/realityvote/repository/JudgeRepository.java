package com.slit.realityvote.repository;

import com.slit.realityvote.entity.Judge;
import com.slit.realityvote.entity.JudgeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JudgeRepository extends JpaRepository<Judge, Long> {

    Optional<Judge> findByIdAndDeletedFalse(Long id);

    Optional<Judge> findByEmailIgnoreCaseAndDeletedFalse(String email);

    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    @Query("SELECT j FROM Judge j WHERE j.deleted = false " +
           "AND (:keyword IS NULL OR LOWER(j.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(j.expertiseArea) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR j.status = :status) " +
           "ORDER BY j.fullName ASC")
    Page<Judge> search(@Param("keyword") String keyword,
                        @Param("status") JudgeStatus status,
                        Pageable pageable);
}
