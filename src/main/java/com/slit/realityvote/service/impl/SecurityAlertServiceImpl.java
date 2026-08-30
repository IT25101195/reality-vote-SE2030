package com.slit.realityvote.service.impl;

import com.slit.realityvote.entity.AlertStatus;
import com.slit.realityvote.entity.SecurityAlert;
import com.slit.realityvote.repository.SecurityAlertRepository;
import com.slit.realityvote.service.SecurityAlertService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SecurityAlertServiceImpl implements SecurityAlertService {

    private final SecurityAlertRepository repo;

    @Override
    @Transactional
    public SecurityAlert raise(SecurityAlert alert) {
        return repo.save(alert);
    }

    @Override
    @Transactional
    public SecurityAlert update(Long id, String description, AlertStatus newStatus) {
        SecurityAlert alert = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found: " + id));
        if (description != null && !description.isBlank()) {
            alert.setDescription(description);
        }
        if (newStatus != null) {
            alert.setStatus(newStatus);
        }
        return repo.save(alert);
    }

    @Override
    public List<SecurityAlert> getAll() {
        return repo.findAllByOrderByCreatedDateDesc();
    }

    @Override
    public Optional<SecurityAlert> getById(Long id) {
        return repo.findById(id);
    }

    @Override
    public List<SecurityAlert> getBySession(Long sessionId) {
        return repo.findBySessionId(sessionId);
    }

    @Override
    public long countOpen() {
        return repo.countByStatus(AlertStatus.OPEN);
    }

    @Override
    public long countByStatus(AlertStatus status) {
        return repo.countByStatus(status);
    }
}
