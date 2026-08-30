package com.slit.realityvote.controller;

import com.slit.realityvote.entity.User;
import com.slit.realityvote.service.AuthBridgeService;
import com.slit.realityvote.service.SupportTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Viewer-facing: submit a complaint/support request, track its status.
 * Matches "Users should be able to view the status of their support
 * requests and receive updates until the issue is resolved."
 */
@Controller
@RequestMapping("/support/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final SupportTicketService ticketService;
    private final AuthBridgeService authBridgeService;

    @GetMapping
    public String myTickets(Authentication authentication, Model model) {
        User user = authBridgeService.resolveCurrentUser(authentication);
        model.addAttribute("tickets", ticketService.getTicketsForUser(user.getId()));
        return "support/tickets/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("subject", "");
        model.addAttribute("description", "");
        return "support/tickets/form";
    }

    @PostMapping
    public String submit(@RequestParam String subject, @RequestParam String description,
                          Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = authBridgeService.resolveCurrentUser(authentication);
        var ticket = ticketService.submitTicket(subject, description, user.getId());
        redirectAttributes.addFlashAttribute("successMessage",
                "Your ticket " + ticket.getReference() + " has been submitted. Our support team will respond soon.");
        return "redirect:/support/tickets";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Authentication authentication, Model model) {
        User user = authBridgeService.resolveCurrentUser(authentication);
        var ticket = ticketService.getById(id);
        if (!ticket.getSubmittedBy().getId().equals(user.getId())) {
            // Viewers can only see their own tickets - not staff-only data,
            // just basic access control so ticket IDs aren't guessable.
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only view your own support tickets.");
        }
        model.addAttribute("ticket", ticket);
        return "support/tickets/view";
    }
}
