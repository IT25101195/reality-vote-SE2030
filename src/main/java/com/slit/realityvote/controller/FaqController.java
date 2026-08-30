package com.slit.realityvote.controller;

import com.slit.realityvote.entity.Faq;
import com.slit.realityvote.service.FaqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    // Public - anyone can browse FAQs, grouped by category
    @GetMapping("/faq")
    public String publicList(Model model) {
        Map<String, List<Faq>> byCategory = faqService.getAll().stream()
                .collect(Collectors.groupingBy(Faq::getCategory, TreeMap::new, Collectors.toList()));
        model.addAttribute("faqsByCategory", byCategory);
        return "faq/public";
    }

    // ---- Staff management (Support Staff / Administrator) ----

    @GetMapping("/staff/faqs")
    public String manage(Model model) {
        model.addAttribute("faqs", faqService.getAll());
        return "faq/manage";
    }

    @GetMapping("/staff/faqs/new")
    public String newForm(Model model) {
        model.addAttribute("faq", new Faq());
        model.addAttribute("isEdit", false);
        return "faq/form";
    }

    @PostMapping("/staff/faqs")
    public String create(@Valid @ModelAttribute("faq") Faq faq, BindingResult result, Model model,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "faq/form";
        }
        faqService.create(faq);
        redirectAttributes.addFlashAttribute("successMessage", "FAQ added.");
        return "redirect:/staff/faqs";
    }

    @GetMapping("/staff/faqs/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("faq", faqService.getById(id));
        model.addAttribute("isEdit", true);
        return "faq/form";
    }

    @PostMapping("/staff/faqs/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("faq") Faq faq, BindingResult result,
                          Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("isEdit", true);
            return "faq/form";
        }
        faqService.update(id, faq);
        redirectAttributes.addFlashAttribute("successMessage", "FAQ updated.");
        return "redirect:/staff/faqs";
    }

    @PostMapping("/staff/faqs/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        faqService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "FAQ deleted.");
        return "redirect:/staff/faqs";
    }
}
