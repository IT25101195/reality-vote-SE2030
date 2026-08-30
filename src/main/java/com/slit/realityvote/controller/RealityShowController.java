package com.slit.realityvote.controller;

import com.slit.realityvote.entity.Episode;
import com.slit.realityvote.entity.RealityShow;
import com.slit.realityvote.entity.Season;
import com.slit.realityvote.service.FileStorageService;
import com.slit.realityvote.service.RealityShowService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles all HTTP requests for the Reality Show Management module.
 * Route access (Administrator-only) is enforced centrally in
 * SecurityConfig via an antMatchers-style rule on "/admin/shows/**",
 * not re-checked here - keeps the controller focused on flow, not auth.
 */
@Controller
@RequestMapping("/admin/shows")
@RequiredArgsConstructor
public class RealityShowController {

    private final RealityShowService showService;
    private final FileStorageService fileStorageService;

    // LIST + SEARCH
    @GetMapping
    public String listShows(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("shows", showService.searchShows(keyword));
        model.addAttribute("keyword", keyword);
        return "shows/list";
    }

    // CREATE - show empty form
    @GetMapping("/new")
    public String newShowForm(Model model) {
        model.addAttribute("show", new RealityShow());
        model.addAttribute("isEdit", false);
        return "shows/form";
    }

    // CREATE - handle submit
    @PostMapping
    public String createShow(@Valid @ModelAttribute("show") RealityShow show,
                              BindingResult result,
                              @RequestParam(required = false) MultipartFile poster,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "shows/form";
        }
        try {
            if (poster != null && !poster.isEmpty()) {
                show.setPosterImagePath(fileStorageService.storeShowPoster(poster));
            }
            showService.createShow(show);
            redirectAttributes.addFlashAttribute("successMessage", "Reality show created successfully.");
            return "redirect:/admin/shows";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", false);
            return "shows/form";
        }
    }

    // EDIT - show pre-filled form
    @GetMapping("/{id}/edit")
    public String editShowForm(@PathVariable Long id, Model model) {
        model.addAttribute("show", showService.getShowById(id));
        model.addAttribute("isEdit", true);
        return "shows/form";
    }

    // EDIT - handle submit
    @PostMapping("/{id}")
    public String updateShow(@PathVariable Long id,
                              @Valid @ModelAttribute("show") RealityShow show,
                              BindingResult result,
                              @RequestParam(required = false) MultipartFile poster,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", true);
            return "shows/form";
        }
        try {
            if (poster != null && !poster.isEmpty()) {
                show.setPosterImagePath(fileStorageService.storeShowPoster(poster));
            }
            showService.updateShow(id, show);
            redirectAttributes.addFlashAttribute("successMessage", "Reality show updated successfully.");
            return "redirect:/admin/shows";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", true);
            return "shows/form";
        }
    }

    // DELETE (soft delete / archive)
    @PostMapping("/{id}/delete")
    public String deleteShow(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        showService.deleteShow(id);
        redirectAttributes.addFlashAttribute("successMessage", "Reality show archived.");
        return "redirect:/admin/shows";
    }

    // VIEW single show with its seasons/episodes
    @GetMapping("/{id}")
    public String viewShow(@PathVariable Long id, Model model) {
        model.addAttribute("show", showService.getShowById(id));
        return "shows/view";
    }

    // ── Seasons ──────────────────────────────────────────────

    @GetMapping("/{id}/seasons/new")
    public String newSeasonForm(@PathVariable Long id, Model model) {
        model.addAttribute("show", showService.getShowById(id));
        model.addAttribute("season", new Season());
        return "shows/season-form";
    }

    @PostMapping("/{id}/seasons")
    public String createSeason(@PathVariable Long id,
                                @Valid @ModelAttribute("season") Season season,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("show", showService.getShowById(id));
            return "shows/season-form";
        }
        showService.addSeasonToShow(id, season);
        redirectAttributes.addFlashAttribute("successMessage", "Season added.");
        return "redirect:/admin/shows/" + id;
    }

    // ── Episodes ─────────────────────────────────────────────

    @GetMapping("/{showId}/seasons/{seasonId}/episodes/new")
    public String newEpisodeForm(@PathVariable Long showId, @PathVariable Long seasonId, Model model) {
        RealityShow show = showService.getShowById(showId);
        Season season = findSeason(show, seasonId);
        model.addAttribute("show", show);
        model.addAttribute("season", season);
        model.addAttribute("episode", new Episode());
        return "shows/episode-form";
    }

    @PostMapping("/{showId}/seasons/{seasonId}/episodes")
    public String createEpisode(@PathVariable Long showId, @PathVariable Long seasonId,
                                 @Valid @ModelAttribute("episode") Episode episode,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            RealityShow show = showService.getShowById(showId);
            model.addAttribute("show", show);
            model.addAttribute("season", findSeason(show, seasonId));
            return "shows/episode-form";
        }
        showService.addEpisodeToSeason(seasonId, episode);
        redirectAttributes.addFlashAttribute("successMessage", "Episode added.");
        return "redirect:/admin/shows/" + showId;
    }

    private Season findSeason(RealityShow show, Long seasonId) {
        return show.getSeasons().stream()
                .filter(s -> s.getId().equals(seasonId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Season not found with id: " + seasonId));
    }
}
