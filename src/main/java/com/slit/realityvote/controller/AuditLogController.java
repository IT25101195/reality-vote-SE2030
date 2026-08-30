package com.slit.realityvote.controller;

import com.slit.realityvote.dto.AuditSearchCriteria;
import com.slit.realityvote.entity.AuditEventType;
import com.slit.realityvote.service.AuditLogService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

/**
 * Read-only for the Compliance Officer (and Administrator) — matches
 * "Only authorized administrators and compliance officers should be
 * able to access detailed audit logs." There is deliberately no
 * edit/delete endpoint anywhere in this controller: an audit trail that
 * can be altered isn't an audit trail.
 *
 * Module 6.4 adds:
 *   GET /compliance/audit-logs/{id}               — single-entry detail view
 *   GET /compliance/audit-logs/entity/{type}/{id} — all entries for one entity
 *   GET /compliance/audit-logs/export             — CSV download
 */
@Controller
@RequestMapping("/compliance/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRATOR','COMPLIANCE_OFFICER')")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private static final int PAGE_SIZE = 15;

    // 1. Paginated list with filters ─────────────────────────────────────────

    @GetMapping
    public String list(@RequestParam(required = false) AuditEventType eventType,
                        @RequestParam(defaultValue = "false") boolean flaggedOnly,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("createdDate").descending());
        var results = auditLogService.search(eventType, flaggedOnly, keyword, pageable);

        model.addAttribute("logPage", results);
        model.addAttribute("eventType", eventType);
        model.addAttribute("flaggedOnly", flaggedOnly);
        model.addAttribute("keyword", keyword);
        model.addAttribute("eventTypes", AuditEventType.values());
        model.addAttribute("flaggedCount", auditLogService.countFlagged());
        return "audit/list";
    }

    // 2. Single audit log entry detail ─────────────────────────────────────────

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        auditLogService.getById(id).ifPresent(log -> model.addAttribute("log", log));
        return "audit/detail";
    }

    // 3. All entries for a specific domain entity ───────────────────────────────

    @GetMapping("/entity/{entityType}/{entityId}")
    public String byEntity(@PathVariable String entityType,
                            @PathVariable Long entityId,
                            Model model) {
        model.addAttribute("logs", auditLogService.getByEntity(entityType, entityId));
        model.addAttribute("entityType", entityType);
        model.addAttribute("entityId", entityId);
        return "audit/list-entity";
    }

    // 4. CSV export ────────────────────────────────────────────────────────────

    @GetMapping("/export")
    public void export(@RequestParam(required = false) AuditEventType eventType,
                        @RequestParam(defaultValue = "false") boolean flaggedOnly,
                        @RequestParam(required = false) String keyword,
                        HttpServletResponse response) throws IOException {
        AuditSearchCriteria criteria = new AuditSearchCriteria(
                eventType, flaggedOnly, keyword, null, null, null, null);
        auditLogService.export(criteria, response);
    }
}
