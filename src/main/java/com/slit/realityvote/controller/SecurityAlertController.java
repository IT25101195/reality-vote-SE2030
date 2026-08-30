package com.slit.realityvote.controller;

import com.slit.realityvote.entity.AlertSeverity;
import com.slit.realityvote.entity.AlertStatus;
import com.slit.realityvote.entity.SecurityAlert;
import com.slit.realityvote.repository.VotingSessionRepository;
import com.slit.realityvote.service.SecurityAlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * SecurityAlert CRUD — Compliance Officer / Administrator.
 *
 * No DELETE endpoint is exposed here, by design:
 * alerts are evidence and must be retained once raised.
 * The create → update (status) lifecycle is enforced via the service layer.
 */
@Controller
@RequestMapping("/compliance/alerts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRATOR','COMPLIANCE_OFFICER')")
public class SecurityAlertController {

    private final SecurityAlertService alertService;
    private final VotingSessionRepository sessionRepository;

    // List ───────────────────────────────────────────────────────────────────

    @GetMapping
    public String list(Model model) {
        model.addAttribute("alerts", alertService.getAll());
        model.addAttribute("openCount", alertService.countOpen());
        model.addAttribute("investigatingCount",
                alertService.countByStatus(AlertStatus.INVESTIGATING));
        model.addAttribute("resolvedCount",
                alertService.countByStatus(AlertStatus.RESOLVED));
        return "compliance/alerts/list";
    }

    // Create ─────────────────────────────────────────────────────────────────

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long sessionId,
                          @RequestParam(required = false) String actorEmail,
                          Model model) {
        SecurityAlert blank = SecurityAlert.builder()
                .sessionId(sessionId)
                .relatedActorEmail(actorEmail)
                .build();
        model.addAttribute("alert", blank);
        model.addAttribute("severities", AlertSeverity.values());
        model.addAttribute("sessions", sessionRepository.findAllByOrderByStartTimeDesc());
        return "compliance/alerts/new";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("alert") SecurityAlert alert,
                         BindingResult br,
                         Authentication auth,
                         Model model,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("severities", AlertSeverity.values());
            model.addAttribute("sessions", sessionRepository.findAllByOrderByStartTimeDesc());
            return "compliance/alerts/new";
        }
        alert.setRaisedByEmail(auth.getName());
        alert.setStatus(AlertStatus.OPEN);
        SecurityAlert saved = alertService.raise(alert);
        ra.addFlashAttribute("flash", "Alert #" + saved.getId() + " raised successfully.");
        return "redirect:/compliance/alerts/" + saved.getId();
    }

    // Detail ─────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        SecurityAlert alert = alertService.getById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Alert not found: " + id));
        model.addAttribute("alert", alert);
        model.addAttribute("statuses", AlertStatus.values());
        return "compliance/alerts/detail";
    }

    // Edit / Update status ────────────────────────────────────────────────────

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        SecurityAlert alert = alertService.getById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Alert not found: " + id));
        model.addAttribute("alert", alert);
        model.addAttribute("statuses", AlertStatus.values());
        model.addAttribute("severities", AlertSeverity.values());
        return "compliance/alerts/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String description,
                         @RequestParam AlertStatus status,
                         RedirectAttributes ra) {
        alertService.update(id, description, status);
        ra.addFlashAttribute("flash", "Alert updated.");
        return "redirect:/compliance/alerts/" + id;
    }
}
