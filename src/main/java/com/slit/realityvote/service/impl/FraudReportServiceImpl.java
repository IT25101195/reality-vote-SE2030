package com.slit.realityvote.service.impl;

import com.slit.realityvote.dto.AnomalyReport;
import com.slit.realityvote.entity.FraudReport;
import com.slit.realityvote.entity.FraudReportStatus;
import com.slit.realityvote.repository.FraudReportRepository;
import com.slit.realityvote.service.ComplianceService;
import com.slit.realityvote.service.FraudReportService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FraudReportServiceImpl implements FraudReportService {

    private final FraudReportRepository repo;
    private final ComplianceService complianceService;

    @Override
    @Transactional
    public FraudReport create(Long sessionId, String raisedByEmail) {
        AnomalyReport anomaly = complianceService.detectAnomalies(sessionId);

        String patterns = String.join("\n", anomaly.anomalyDescriptions());
        String actors   = String.join(", ", anomaly.topSuspiciousActors());

        FraudReport report = FraudReport.builder()
                .sessionId(sessionId)
                .sessionDescription(anomaly.sessionDescription())
                .summary("Auto-generated from anomaly detection on " + anomaly.sessionDescription())
                .detectedPatterns(patterns)
                .suspiciousActors(actors)
                .totalFlaggedEvents(anomaly.totalFlaggedEvents())
                .raisedByEmail(raisedByEmail)
                .status(FraudReportStatus.DRAFT)
                .build();

        return repo.save(report);
    }

    @Override
    @Transactional
    public FraudReport updateSummary(Long id, String summary) {
        FraudReport report = get(id);
        report.setSummary(summary);
        return repo.save(report);
    }

    @Override
    @Transactional
    public FraudReport advance(Long id) {
        FraudReport report = get(id);
        switch (report.getStatus()) {
            case DRAFT        -> report.setStatus(FraudReportStatus.FILED);
            case FILED        -> report.setStatus(FraudReportStatus.UNDER_REVIEW);
            case UNDER_REVIEW -> report.setStatus(FraudReportStatus.RESOLVED);
            case RESOLVED     -> throw new IllegalStateException(
                    "Report #" + id + " is already RESOLVED — cannot advance further.");
        }
        return repo.save(report);
    }

    @Override public List<FraudReport> getAll()                    { return repo.findAllByOrderByCreatedDateDesc(); }
    @Override public Optional<FraudReport> getById(Long id)        { return repo.findById(id); }
    @Override public List<FraudReport> getBySession(Long sessionId){ return repo.findBySessionId(sessionId); }

    private FraudReport get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FraudReport not found: " + id));
    }
}
