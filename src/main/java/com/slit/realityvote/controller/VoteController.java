package com.slit.realityvote.controller;

import com.slit.realityvote.entity.User;
import com.slit.realityvote.entity.VotingSessionStatus;
import com.slit.realityvote.service.AuthBridgeService;
import com.slit.realityvote.service.VoteService;
import com.slit.realityvote.service.VotingSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Viewer-facing voting flow: see sessions currently OPEN, cast a vote,
 * watch live results, view personal voting history.
 *
 * The current user's `users` table row is resolved via AuthBridgeService
 * (see that class for why a bridge is needed at all).
 */
@Controller
@RequestMapping("/vote")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;
    private final VotingSessionService sessionService;
    private final AuthBridgeService authBridgeService;

    @GetMapping
    public String openSessions(Model model) {
        model.addAttribute("sessions", sessionService.getSessionsByStatus(VotingSessionStatus.OPEN));
        return "vote/sessions";
    }

    @GetMapping("/{sessionId}")
    public String votingPage(@PathVariable Long sessionId, Model model) {
        model.addAttribute("votingSession", sessionService.getById(sessionId));
        model.addAttribute("results", voteService.getLiveResults(sessionId));
        return "vote/cast";
    }

    @PostMapping("/{sessionId}/contestants/{contestantId}")
    public String cast(@PathVariable Long sessionId, @PathVariable Long contestantId,
                       Authentication authentication, RedirectAttributes redirectAttributes) {
        User voter = authBridgeService.resolveCurrentUser(authentication);
        try {
            voteService.castVote(sessionId, contestantId, voter.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Your vote has been successfully recorded.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/vote/" + sessionId;
    }

    @GetMapping("/history")
    public String history(Authentication authentication, Model model) {
        User voter = authBridgeService.resolveCurrentUser(authentication);
        model.addAttribute("votes", voteService.getVotingHistoryForVoter(voter.getId()));
        return "vote/history";
    }
}