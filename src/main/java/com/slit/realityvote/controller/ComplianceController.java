package com.slit.realityvote.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slit.realityvote.dto.*;
import com.slit.realityvote.entity.ReportStatus;
import com.slit.realityvote.repository.VotingSessionRepository;
import com.slit.realityvote.service.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Module 6.4 — Voting Compliance & Security controller.
 *
 * Hard rule that applies to every method in this class:
 * No code path writes to Vote or AuditLog for a SECURITY_OFFICER token.
 * All write operations here target SecurityAlert, FraudReport, or
 * ComplianceReportRecord — the three artifacts the officer is permitted
 * to create and update per the design-level rights matrix.
 */
@Controller
@RequestMapping("/compliance")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMINISTRATOR','COMPLIANCE_OFFICER')")
public class ComplianceController {

    private final ComplianceService             complianceService;
    private final AuditLogService               auditLogService;
    private final FraudReportService            fraudReportService;
    private final ComplianceReportRecordService reportRecordService;
    private final VotingSessionRepository       sessionRepository;
    private final ObjectMapper                  objectMapper;

    // ── 1. Dashboard ─────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats",      complianceService.getDashboardStats());
        model.addAttribute("topActors",  auditLogService.getTopFlaggedActors(5));
        model.addAttribute("sessions",   sessionRepository.findAllByOrderByStartTimeDesc());
        return "compliance/dashboard";
    }

    // ── 2. Vote Activity (Highcharts chart) ──────────────────────────────────

    @GetMapping("/activity")
    public String activity(@RequestParam(required = false) Long sessionId, Model model) {
        model.addAttribute("sessions", sessionRepository.findAllByOrderByStartTimeDesc());

        if (sessionId != null) {
            VotingActivityStats activity = complianceService.getActivity(sessionId);
            model.addAttribute("activity", activity);
            model.addAttribute("selectedSessionId", sessionId);

            try {
                List<Map<String, Object>> chartData = activity.contestantStats().stream()
                        .map(c -> Map.<String, Object>of(
                                "id",      c.id(),
                                "name",    c.fullName(),
                                "y",       c.voteCount(),
                                "color",   c.barColor(),
                                "status",  c.status().name(),
                                "bio",     c.biography().length() > 200
                                               ? c.biography().substring(0, 200) + "\u2026"
                                               : c.biography(),
                                "talent",  c.talentCategory(),
                                "town",    c.hometown(),
                                "flagged", c.flaggedByAnomaly()))
                        .collect(Collectors.toList());

                model.addAttribute("chartDataJson",
                        objectMapper.writeValueAsString(chartData));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialise chart data", e);
                model.addAttribute("chartDataJson", "[]");
            }
        }
        return "compliance/activity";
    }

    // ── 3. Anomaly Detection ──────────────────────────────────────────────────

    @GetMapping("/anomalies")
    public String anomalies(@RequestParam(required = false) Long sessionId, Model model) {
        model.addAttribute("sessions", sessionRepository.findAllByOrderByStartTimeDesc());
        if (sessionId != null) {
            model.addAttribute("anomalyReport",
                    complianceService.detectAnomalies(sessionId));
            model.addAttribute("selectedSessionId", sessionId);
        }
        return "compliance/anomalies";
    }

    // ── 4. Integrity Verification ─────────────────────────────────────────────

    @GetMapping("/verify/{sessionId}")
    public String verify(@PathVariable Long sessionId, Model model) {
        model.addAttribute("integrity",
                complianceService.verifyIntegrity(sessionId));
        model.addAttribute("sessions",
                sessionRepository.findAllByOrderByStartTimeDesc());
        return "compliance/verify";
    }

    // ── 5. Live Compliance Report ─────────────────────────────────────────────

    @GetMapping("/reports/{sessionId}")
    public String report(@PathVariable Long sessionId, Model model) {
        model.addAttribute("report", complianceService.generateReport(sessionId));
        model.addAttribute("existingRecords",
                reportRecordService.getBySession(sessionId));
        return "compliance/report";
    }

    // ── 6. Save compliance report as a DRAFT record ───────────────────────────

    @PostMapping("/reports/{sessionId}/save")
    public String saveReport(@PathVariable Long sessionId,
                             Authentication auth,
                             RedirectAttributes ra) {
        var record = reportRecordService.generate(sessionId, auth.getName());
        ra.addFlashAttribute("flash",
                "Report saved as Draft #" + record.getId() + ".");
        return "redirect:/compliance/saved-reports/" + record.getId();
    }

    // ── 7. Saved Reports — list ───────────────────────────────────────────────

    @GetMapping("/saved-reports")
    public String savedReports(Model model) {
        model.addAttribute("records", reportRecordService.getAll());
        return "compliance/saved-reports/list";
    }

    // ── 8. Saved Report — detail ──────────────────────────────────────────────

    @GetMapping("/saved-reports/{id}")
    public String savedReportDetail(@PathVariable Long id, Model model) {
        var record = reportRecordService.getById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Report not found: " + id));
        model.addAttribute("record", record);
        return "compliance/saved-reports/detail";
    }

    // ── 9. File a saved report (DRAFT → FILED) ────────────────────────────────

    @PostMapping("/saved-reports/{id}/file")
    public String fileReport(@PathVariable Long id, RedirectAttributes ra) {
        reportRecordService.file(id);
        ra.addFlashAttribute("flash", "Report #" + id + " has been officially filed.");
        return "redirect:/compliance/saved-reports/" + id;
    }

    // ── 10. Fraud Reports — list ──────────────────────────────────────────────

    @GetMapping("/fraud-reports")
    public String fraudReports(Model model) {
        model.addAttribute("reports", fraudReportService.getAll());
        return "compliance/fraud-reports/list";
    }

    // ── 11. Fraud Report — detail ─────────────────────────────────────────────

    @GetMapping("/fraud-reports/{id}")
    public String fraudReportDetail(@PathVariable Long id, Model model) {
        var report = fraudReportService.getById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Fraud report not found: " + id));
        model.addAttribute("report", report);
        return "compliance/fraud-reports/detail";
    }

    // ── 12. Save anomaly result as a FraudReport ──────────────────────────────

    @PostMapping("/fraud-reports")
    public String createFraudReport(@RequestParam Long sessionId,
                                    Authentication auth,
                                    RedirectAttributes ra) {
        var report = fraudReportService.create(sessionId, auth.getName());
        ra.addFlashAttribute("flash",
                "Fraud report #" + report.getId() + " created (DRAFT).");
        return "redirect:/compliance/fraud-reports/" + report.getId();
    }

    // ── 13. Advance fraud report status ──────────────────────────────────────

    @PostMapping("/fraud-reports/{id}/advance")
    public String advanceFraudReport(@PathVariable Long id, RedirectAttributes ra) {
        var report = fraudReportService.advance(id);
        ra.addFlashAttribute("flash",
                "Report advanced to " + report.getStatus() + ".");
        return "redirect:/compliance/fraud-reports/" + id;
    }

    // ── 14. Update fraud report summary ──────────────────────────────────────

    @PostMapping("/fraud-reports/{id}/summary")
    public String updateFraudSummary(@PathVariable Long id,
                                     @RequestParam String summary,
                                     RedirectAttributes ra) {
        fraudReportService.updateSummary(id, summary);
        ra.addFlashAttribute("flash", "Summary updated.");
        return "redirect:/compliance/fraud-reports/" + id;
    }

    // ── 15. Voting Rules read-view ────────────────────────────────────────────

    @GetMapping("/voting-rules")
    public String votingRules(Model model) {
        model.addAttribute("sessions",
                sessionRepository.findAllByOrderByStartTimeDesc());
        return "compliance/voting-rules";
    }

    // ── 16. Export compliance report CSV ──────────────────────────────────────

    @GetMapping("/reports/{sessionId}/export")
    public void exportReport(@PathVariable Long sessionId,
                              HttpServletResponse response) throws IOException {
        AuditSearchCriteria criteria = new AuditSearchCriteria(
                null, false, null, "VotingSession", sessionId, null, null);
        auditLogService.export(criteria, response);
    }

    // ── 17. Purge old audit entries (ADMINISTRATOR only) ──────────────────────

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PostMapping("/audit-logs/purge")
    public String purge(@RequestParam int daysOld, RedirectAttributes ra) {
        LocalDateTime before = LocalDateTime.now().minusDays(daysOld);
        int deleted = auditLogService.purge(before);
        ra.addFlashAttribute("purgeMessage",
                deleted + " non-vote audit entries older than " + daysOld
                        + " days removed.");
        return "redirect:/compliance/audit-logs";
    }
}
