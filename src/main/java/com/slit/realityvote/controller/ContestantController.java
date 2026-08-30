package com.slit.realityvote.controller;

import com.slit.realityvote.dto.SeasonOption;
import com.slit.realityvote.entity.Contestant;
import com.slit.realityvote.entity.ContestantStatus;
import com.slit.realityvote.repository.SeasonRepository;
import com.slit.realityvote.service.ContestantService;
import com.slit.realityvote.service.RealityShowService;
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

import java.util.List;

/**
 * Handles Contestant Management. Access is restricted to
 * ADMINISTRATOR and CONTESTANT_STAFF roles (see SecurityConfig) -
 * matching "Only authorized administrators and contestant management
 * staff should be allowed to add new contestants" from the requirements.
 */
@Controller
@RequestMapping("/staff/contestants")
@RequiredArgsConstructor
public class ContestantController {

    private final ContestantService contestantService;
    private final RealityShowService showService;
    private final SeasonRepository seasonRepository;

    private static final int PAGE_SIZE = 9;

    // AJAX: populate the season dropdown once a show is chosen in the form
    @GetMapping("/seasons-by-show")
    @ResponseBody
    public List<SeasonOption> seasonsByShow(@RequestParam Long showId) {
        return seasonRepository.findByShowIdOrderBySeasonNumberAsc(showId).stream()
                .map(s -> new SeasonOption(s.getId(), s.getSeasonNumber()))
                .toList();
    }

    // LIST + SEARCH + FILTER + PAGINATION
    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                        @RequestParam(required = false) Long showId,
                        @RequestParam(required = false) ContestantStatus status,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("fullName").ascending());
        Page<Contestant> result = contestantService.search(keyword, showId, status, pageable);

        model.addAttribute("contestantPage", result);
        model.addAttribute("keyword", keyword);
        model.addAttribute("showId", showId);
        model.addAttribute("status", status);
        model.addAttribute("shows", showService.getAllActiveShows());
        model.addAttribute("statuses", ContestantStatus.values());
        return "contestants/list";
    }

    // CREATE - form
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("contestant", new Contestant());
        model.addAttribute("shows", showService.getAllActiveShows());
        model.addAttribute("isEdit", false);
        return "contestants/form";
    }

    // CREATE - submit
    @PostMapping
    public String create(@Valid @ModelAttribute("contestant") Contestant contestant,
                          BindingResult result,
                          @RequestParam Long showId,
                          @RequestParam(required = false) Long seasonId,
                          @RequestParam(required = false) MultipartFile photo,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("shows", showService.getAllActiveShows());
            model.addAttribute("isEdit", false);
            return "contestants/form";
        }
        try {
            contestantService.createContestant(contestant, showId, seasonId, photo);
            redirectAttributes.addFlashAttribute("successMessage", "Contestant added successfully.");
            return "redirect:/staff/contestants";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("shows", showService.getAllActiveShows());
            model.addAttribute("isEdit", false);
            return "contestants/form";
        }
    }

    // EDIT - form
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("contestant", contestantService.getById(id));
        model.addAttribute("shows", showService.getAllActiveShows());
        model.addAttribute("isEdit", true);
        return "contestants/form";
    }

    // EDIT - submit
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("contestant") Contestant contestant,
                          BindingResult result,
                          @RequestParam Long showId,
                          @RequestParam(required = false) Long seasonId,
                          @RequestParam(required = false) MultipartFile photo,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("shows", showService.getAllActiveShows());
            model.addAttribute("isEdit", true);
            return "contestants/form";
        }
        try {
            contestantService.updateContestant(id, contestant, showId, seasonId, photo);
            redirectAttributes.addFlashAttribute("successMessage", "Contestant updated successfully.");
            return "redirect:/staff/contestants";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("shows", showService.getAllActiveShows());
            model.addAttribute("isEdit", true);
            return "contestants/form";
        }
    }

    // VIEW single contestant
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("contestant", contestantService.getById(id));
        return "contestants/view";
    }

    // STATUS UPDATE (Active / Eliminated / Withdrawn / Winner)
    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                                @RequestParam ContestantStatus newStatus,
                                RedirectAttributes redirectAttributes) {
        contestantService.updateStatus(id, newStatus);
        redirectAttributes.addFlashAttribute("successMessage", "Status updated to " + newStatus + ".");
        return "redirect:/staff/contestants/" + id;
    }

    // DEACTIVATE (soft delete)
    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        contestantService.deactivateContestant(id);
        redirectAttributes.addFlashAttribute("successMessage", "Contestant deactivated.");
        return "redirect:/staff/contestants";
    }
}
