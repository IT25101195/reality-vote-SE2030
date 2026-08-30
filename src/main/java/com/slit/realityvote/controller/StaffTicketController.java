package com.slit.realityvote.controller;

import com.slit.realityvote.entity.TicketStatus;
import com.slit.realityvote.entity.User;
import com.slit.realityvote.service.AuthBridgeService;
import com.slit.realityvote.service.SupportTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Support Staff / Administrator: triage and respond to tickets.
 * "Authorized customer support staff and administrators should be able
 * to investigate, resolve, and close support tickets."
 */
@Controller
@RequestMapping("/staff/support/tickets")
@RequiredArgsConstructor
public class StaffTicketController {

    private final SupportTicketService ticketService;
    private final AuthBridgeService authBridgeService;
    private static final int PAGE_SIZE = 10;

    @GetMapping
    public String list(@RequestParam(required = false) TicketStatus status,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("createdDate").descending());
        model.addAttribute("ticketPage", ticketService.searchForStaff(status, pageable));
        model.addAttribute("status", status);
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("openCount", ticketService.countByStatus(TicketStatus.OPEN));
        return "support/staff/list";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("ticket", ticketService.getById(id));
        model.addAttribute("statuses", TicketStatus.values());
        return "support/staff/view";
    }

    @PostMapping("/{id}/respond")
    public String respond(@PathVariable Long id,
                           @RequestParam String response,
                           @RequestParam TicketStatus newStatus,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        User staff = authBridgeService.resolveCurrentUser(authentication);
        ticketService.respond(id, response, newStatus, staff.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Response sent and ticket marked " + newStatus + ".");
        return "redirect:/staff/support/tickets/" + id;
    }
}
