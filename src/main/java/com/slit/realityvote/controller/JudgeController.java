package com.slit.realityvote.controller;

import com.slit.realityvote.entity.Judge;
import com.slit.realityvote.entity.JudgeStatus;
import com.slit.realityvote.entity.Role;
import com.slit.realityvote.repository.EpisodeRepository;
import com.slit.realityvote.repository.SeasonRepository;
import com.slit.realityvote.repository.UserRepository;
import com.slit.realityvote.service.JudgeAssignmentService;
import com.slit.realityvote.service.JudgeService;
import com.slit.realityvote.service.RealityShowService;
import com.slit.realityvote.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Judge & Panel Management (administrator side): maintaining judge
 * profiles and assigning them onto judging panels for shows, seasons,
 * and episodes. Access is restricted to ADMINISTRATOR (see
 * SecurityConfig) - judges themselves use the separate /judge/scores
 * area (ScoreController) to do their scoring work.
 */
@Controller
@RequestMapping("/admin/judges")
@RequiredArgsConstructor
public class JudgeController {

    private final JudgeService judgeService;
    private final JudgeAssignmentService assignmentService;
    private final RealityShowService showService;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;
    private final UserService userService;
    private final UserRepository userRepository;

    private static final int PAGE_SIZE = 9;

    // LIST + SEARCH + FILTER
    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) JudgeStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("fullName").ascending());
        Page<Judge> result = judgeService.search(keyword, status, pageable);

        model.addAttribute("judgePage", result);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("statuses", JudgeStatus.values());
        return "judges/list";
    }

    // CREATE - form
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("judge", new Judge());
        model.addAttribute("isEdit", false);
        return "judges/form";
    }

    // CREATE - submit
    @PostMapping
    public String create(@Valid @ModelAttribute("judge") Judge judge,
                         BindingResult result,
                         @RequestParam(required = false) String loginPassword,
                         @RequestParam(required = false) MultipartFile photo,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "judges/form";
        }
        try {
            Judge saved = judgeService.createJudge(judge, photo);

            // Optional one-step login creation: the same "Add Judge" form
            // can immediately give this person a JUDGE account so they can
            // sign in and score - no separate admin step required, unlike
            // before when only the seeded Demo Judge could actually log in.
            if (loginPassword != null && !loginPassword.isBlank()) {
                try {
                    userService.createStaffAccount(saved.getFullName(), saved.getEmail(), loginPassword, Role.JUDGE);
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Judge added and login created - they can sign in with " + saved.getEmail() + " now.");
                } catch (IllegalArgumentException loginEx) {
                    redirectAttributes.addFlashAttribute("successMessage", "Judge added successfully.");
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Judge profile saved, but login wasn't created: " + loginEx.getMessage()
                                    + " Use \"Create Login\" on the judge's page instead.");
                }
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Judge added successfully. No login was created yet - use \"Create Login\" on the judge's page so they can sign in.");
            }
            return "redirect:/admin/judges";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", false);
            return "judges/form";
        }
    }

    // EDIT - form
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("judge", judgeService.getById(id));
        model.addAttribute("isEdit", true);
        return "judges/form";
    }

    // EDIT - submit
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("judge") Judge judge,
                         BindingResult result,
                         @RequestParam(required = false) MultipartFile photo,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", true);
            return "judges/form";
        }
        try {
            judgeService.updateJudge(id, judge, photo);
            redirectAttributes.addFlashAttribute("successMessage", "Judge updated successfully.");
            return "redirect:/admin/judges";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", true);
            return "judges/form";
        }
    }

    // VIEW single judge + their panel assignments
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        Judge judge = judgeService.getById(id);
        model.addAttribute("judge", judge);
        model.addAttribute("assignments", assignmentService.getAssignmentsForJudge(id));
        model.addAttribute("shows", showService.getAllActiveShows());
        model.addAttribute("hasLogin", userRepository.findByEmail(judge.getEmail()).isPresent());
        return "judges/view";
    }

    // Creates the JUDGE login account for an existing judge profile that
    // doesn't have one yet (e.g. added before a password was set, or
    // added by an earlier version of this form).
    @PostMapping("/{id}/create-login")
    public String createLogin(@PathVariable Long id,
                              @RequestParam String loginPassword,
                              RedirectAttributes redirectAttributes) {
        Judge judge = judgeService.getById(id);
        try {
            userService.createStaffAccount(judge.getFullName(), judge.getEmail(), loginPassword, Role.JUDGE);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Login created - " + judge.getEmail() + " can now sign in and score contestants.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/judges/" + id;
    }

    // STATUS UPDATE (Active / Inactive)
    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam JudgeStatus newStatus,
                               RedirectAttributes redirectAttributes) {
        judgeService.updateStatus(id, newStatus);
        redirectAttributes.addFlashAttribute("successMessage", "Status updated to " + newStatus + ".");
        return "redirect:/admin/judges/" + id;
    }

    // DEACTIVATE (soft delete)
    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        judgeService.deactivateJudge(id);
        redirectAttributes.addFlashAttribute("successMessage", "Judge deactivated.");
        return "redirect:/admin/judges";
    }

    // AJAX: populate the season + episode dropdowns once a show is chosen
    // on the assign-panel form. Returns plain DTOs (not entities) so we
    // never need to worry about serializing lazy Hibernate proxies.
    @GetMapping("/seasons-by-show")
    @ResponseBody
    public java.util.List<SeasonOptionView> seasonsByShow(@RequestParam Long showId) {
        return seasonRepository.findByShowIdOrderBySeasonNumberAsc(showId).stream()
                .map(s -> new SeasonOptionView(s.getId(), s.getSeasonNumber(),
                        s.getEpisodes().stream()
                                .map(e -> new EpisodeOptionView(e.getId(), e.getEpisodeNumber(), e.getTitle()))
                                .toList()))
                .toList();
    }

    // ---- Panel assignment ----

    @PostMapping("/{id}/assign")
    public String assignToPanel(@PathVariable Long id,
                                @RequestParam Long showId,
                                @RequestParam(required = false) Long seasonId,
                                @RequestParam(required = false) Long episodeId,
                                @RequestParam String panelName,
                                RedirectAttributes redirectAttributes) {
        try {
            assignmentService.assignJudge(id, showId, seasonId, episodeId, panelName);
            redirectAttributes.addFlashAttribute("successMessage", "Judge assigned to panel.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/judges/" + id;
    }

    @PostMapping("/{id}/assignments/{assignmentId}/remove")
    public String removeAssignment(@PathVariable Long id, @PathVariable Long assignmentId,
                                   RedirectAttributes redirectAttributes) {
        assignmentService.removeAssignment(assignmentId);
        redirectAttributes.addFlashAttribute("successMessage", "Panel assignment removed.");
        return "redirect:/admin/judges/" + id;
    }

    // Open/close the judging window for one episode - the "designated
    // judging period" scores can only be submitted/revised in.
    @PostMapping("/episodes/{episodeId}/judging-window")
    public String toggleJudgingWindow(@PathVariable Long episodeId,
                                      @RequestParam boolean open,
                                      @RequestParam Long showId,
                                      RedirectAttributes redirectAttributes) {
        episodeRepository.findById(episodeId).ifPresent(episode -> {
            episode.setJudgingOpen(open);
            episodeRepository.save(episode);
        });
        redirectAttributes.addFlashAttribute("successMessage",
                "Judging window " + (open ? "opened." : "closed."));
        return "redirect:/admin/judges/panels?showId=" + showId;
    }

    // Panels overview for one show: every assignment + a quick toggle for
    // each episode's judging window, so the admin doesn't need to leave
    // the Judge module to control scoring periods.
    @GetMapping("/panels")
    public String panelsForShow(@RequestParam(required = false) Long showId, Model model) {
        model.addAttribute("shows", showService.getAllActiveShows());
        model.addAttribute("selectedShowId", showId);
        if (showId != null) {
            model.addAttribute("selectedShow", showService.getShowById(showId));
            model.addAttribute("assignments", assignmentService.getAssignmentsForShow(showId));
        }
        return "judges/panels";
    }

    // Small inline DTOs for the seasons-by-show AJAX response
    public record SeasonOptionView(Long id, Integer seasonNumber, java.util.List<EpisodeOptionView> episodes) {
    }

    public record EpisodeOptionView(Long id, Integer episodeNumber, String title) {
    }
}