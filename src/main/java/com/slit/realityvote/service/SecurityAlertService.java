package com.slit.realityvote.service;

import com.slit.realityvote.entity.AlertStatus;
import com.slit.realityvote.entity.SecurityAlert;

import java.util.List;
import java.util.Optional;

/**
 * Service for SecurityAlert lifecycle management.
 *
 * Intentionally has NO delete method — deleting an alert would compromise
 * the evidentiary record. The interface itself enforces this constraint.
 */
public interface SecurityAlertService {
    SecurityAlert raise(SecurityAlert alert);
    SecurityAlert update(Long id, String description, AlertStatus newStatus);
    List<SecurityAlert> getAll();
    Optional<SecurityAlert> getById(Long id);
    List<SecurityAlert> getBySession(Long sessionId);
    long countOpen();
    long countByStatus(AlertStatus status);
}
