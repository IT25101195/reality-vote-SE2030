package com.slit.realityvote.controller;

import com.slit.realityvote.entity.VotingSession;
import com.slit.realityvote.service.ContestantService;
import com.slit.realityvote.service.RealityShowService;
import com.slit.realityvote.service.VotingSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Administrator-only: create voting sessions and control their lifecycle
 * (open/close). Casting an actual vote is handled separately in
 * VoteController, which is what Viewers use.
 */
@Controller
@RequestMapping("/admin/voting-sessions")
@RequiredArgsConstructor
public class VotingSessionController {

    private final VotingSessionService sessionService;
    private final RealityShowService showService;
    private final ContestantService contestantService;
    private final com.slit.realityvote.service.VoteService voteService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("sessions", sessionService.getAllSessions());
        return "voting-sessions/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("votingSession", new VotingSession());
        model.addAttribute("shows", showService.getAllActiveShows());
        return "voting-sessions/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("votingSession") VotingSession session,
                         BindingResult result,
                         @RequestParam(required = false) Long episodeId,
                         @RequestParam(required = false) List<Long> contestantIds,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        // episodeId/contestantIds are populated by JS after the admin picks a
        // show, so they arrive as separate request params rather than bound
        // fields on `session`. Making them required=false (instead of plain
        // @RequestParam) stops a missing selection from throwing
        // MissingServletRequestParameterException - which would otherwise
        // skip result.hasErrors() entirely and fall through to the Whitelabel
        // error page. We validate them by hand here instead, same as any
        // other form error.
        if (episodeId == null) {
            result.reject("episodeId", "Please select a show and an episode.");
        }
        if (contestantIds == null || contestantIds.isEmpty()) {
            result.reject("contestantIds", "Select at least one contestant for this voting session.");
        }
        if (result.hasErrors()) {
            model.addAttribute("shows", showService.getAllActiveShows());
            return "voting-sessions/form";
        }
        try {
            sessionService.createSession(episodeId, contestantIds, session);
            redirectAttributes.addFlashAttribute("successMessage", "Voting session created (currently SCHEDULED).");
            return "redirect:/admin/voting-sessions";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("shows", showService.getAllActiveShows());
            return "voting-sessions/form";
        }
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        VotingSession session = sessionService.getById(id);
        model.addAttribute("votingSession", session);
        model.addAttribute("results", voteService.getLiveResults(id));
        return "voting-sessions/view";
    }

    @PostMapping("/{id}/open")
    public String open(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            sessionService.openSession(id);
            redirectAttributes.addFlashAttribute("successMessage", "Voting session is now OPEN.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/voting-sessions/" + id;
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        sessionService.closeSession(id);
        redirectAttributes.addFlashAttribute("successMessage", "Voting session is now CLOSED.");
        return "redirect:/admin/voting-sessions/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        sessionService.deleteSession(id);
        redirectAttributes.addFlashAttribute("successMessage", "Voting session deleted.");
        return "redirect:/admin/voting-sessions";
    }

    // AJAX: episodes for a chosen show, then contestants for a chosen episode's show
    @GetMapping("/episodes-by-show")
    @ResponseBody
    public List<EpisodeOption> episodesByShow(@RequestParam Long showId) {
        return showService.getShowById(showId).getSeasons().stream()
                .flatMap(season -> season.getEpisodes().stream())
                .map(e -> new EpisodeOption(e.getId(), e.getEpisodeNumber(), e.getTitle()))
                .toList();
    }

    @GetMapping("/contestants-by-show")
    @ResponseBody
    public List<ContestantOption> contestantsByShow(@RequestParam Long showId) {
        return contestantService
                .search(null, showId, null, PageRequest.of(0, 100))
                .getContent().stream()
                .map(c -> new ContestantOption(c.getId(), c.getFullName()))
                .toList();
    }

    public record EpisodeOption(Long id, Integer episodeNumber, String title) {}
    public record ContestantOption(Long id, String fullName) {}
}