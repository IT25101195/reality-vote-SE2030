package com.slit.realityvote.controller;

import com.slit.realityvote.entity.Score;
import com.slit.realityvote.repository.EpisodeRepository;
import com.slit.realityvote.service.ScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Judge-facing scoring flow: a logged-in JUDGE sees the episodes they're
 * assigned to (via a JudgeAssignment panel), scores the contestants in
 * each one, and reviews their own scoring history. Mirrors the shape of
 * VoteController but for the judging side of the system.
 *
 * The judge's identity is resolved straight from Authentication#getName()
 * (the login email) rather than AuthBridgeService, because Judge is its
 * own profile entity - not a `users` row - matched by email (see Judge
 * entity javadoc).
 */
@Controller
@RequestMapping("/judge")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;
    private final EpisodeRepository episodeRepository;

    @GetMapping("/scores")
    public String assignedEpisodes(Authentication authentication, Model model) {
        String email = authentication.getName();
        model.addAttribute("episodes", scoreService.getAssignedEpisodes(email));
        return "judge/scores/list";
    }

    @GetMapping("/scores/episodes/{episodeId}")
    public String episodeContestants(@PathVariable Long episodeId, Authentication authentication, Model model) {
        String email = authentication.getName();
        model.addAttribute("episodeId", episodeId);
        model.addAttribute("episode", episodeRepository.findById(episodeId).orElse(null));
        var contestants = scoreService.getScorableContestants(email, episodeId);
        model.addAttribute("contestants", contestants);

        // Pre-load any existing score per contestant so the form can show "revise" instead of "submit"
        var existingScores = new java.util.HashMap<Long, Score>();
        for (var c : contestants) {
            Score existing = scoreService.getExistingScore(email, c.getId(), episodeId);
            if (existing != null) {
                existingScores.put(c.getId(), existing);
            }
        }
        model.addAttribute("existingScores", existingScores);
        return "judge/scores/episode";
    }

    @PostMapping("/scores/episodes/{episodeId}/contestants/{contestantId}")
    public String submitScore(@PathVariable Long episodeId, @PathVariable Long contestantId,
                               @RequestParam Integer scoreValue,
                               @RequestParam(required = false) String remarks,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        try {
            scoreService.submitScore(email, contestantId, episodeId, scoreValue, remarks);
            redirectAttributes.addFlashAttribute("successMessage", "Score saved.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/judge/scores/episodes/" + episodeId;
    }

    @GetMapping("/scores/history")
    public String history(Authentication authentication, Model model) {
        String email = authentication.getName();
        model.addAttribute("scores", scoreService.getScoringHistory(email));
        return "judge/scores/history";
    }
}
