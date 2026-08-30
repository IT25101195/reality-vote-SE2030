package com.slit.realityvote.service.impl;

import com.slit.realityvote.dto.ComplianceReportDto;
import com.slit.realityvote.entity.ComplianceReportRecord;
import com.slit.realityvote.entity.ReportStatus;
import com.slit.realityvote.repository.ComplianceReportRecordRepository;
import com.slit.realityvote.service.ComplianceReportRecordService;
import com.slit.realityvote.service.ComplianceService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplianceReportRecordServiceImpl implements ComplianceReportRecordService {

    private final ComplianceReportRecordRepository repo;
    private final ComplianceService complianceService;

    @Override
    @Transactional
    public ComplianceReportRecord generate(Long sessionId, String raisedByEmail) {
        ComplianceReportDto dto = complianceService.generateReport(sessionId);

        String recs = String.join("\n", dto.recommendations());

        ComplianceReportRecord record = ComplianceReportRecord.builder()
                .sessionId(sessionId)
                .sessionDescription(dto.sessionDescription())
                .generatedAt(dto.reportGeneratedAt())
                .integrityStatus(dto.integrityReport().integrityStatus())
                .totalVotes(dto.activityStats().totalVotes())
                .totalFlagged(dto.integrityReport().auditedVoteCount())
                .totalRejected(dto.integrityReport().rejectedVoteCount())
                .recommendations(recs)
                .status(ReportStatus.DRAFT)
                .raisedByEmail(raisedByEmail)
                .build();

        return repo.save(record);
    }

    @Override
    @Transactional
    public ComplianceReportRecord file(Long id) {
        ComplianceReportRecord record = get(id);
        if (record.getStatus() == ReportStatus.FILED) {
            throw new IllegalStateException("Report #" + id + " is already filed.");
        }
        record.setStatus(ReportStatus.FILED);
        record.setFiledAt(LocalDateTime.now());
        return repo.save(record);
    }

    @Override public List<ComplianceReportRecord> getAll()                    { return repo.findAllByOrderByCreatedDateDesc(); }
    @Override public Optional<ComplianceReportRecord> getById(Long id)        { return repo.findById(id); }
    @Override public List<ComplianceReportRecord> getBySession(Long sessionId){ return repo.findBySessionId(sessionId); }

    private ComplianceReportRecord get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ComplianceReportRecord not found: " + id));
    }
}
