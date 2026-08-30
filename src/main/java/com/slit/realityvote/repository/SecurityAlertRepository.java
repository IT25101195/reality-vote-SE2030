package com.slit.realityvote.repository;

import com.slit.realityvote.entity.AlertStatus;
import com.slit.realityvote.entity.SecurityAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {
    List<SecurityAlert> findAllByOrderByCreatedDateDesc();
    List<SecurityAlert> findByStatus(AlertStatus status);
    List<SecurityAlert> findBySessionId(Long sessionId);
    long countByStatus(AlertStatus status);
}
