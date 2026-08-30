package com.slit.realityvote.controller;

import com.slit.realityvote.dto.PasswordChangeForm;
import com.slit.realityvote.dto.ProfileForm;
import com.slit.realityvote.entity.User;
import com.slit.realityvote.service.AuthBridgeService;
import com.slit.realityvote.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * "Manage my account" page - available to every logged-in user regardless
 * of role (VIEWER, ADMINISTRATOR, staff, etc. all have a profile). Lets
 * someone update their name/email and change their password. This is
 * separate from admin-side user/contestant management, which stays
 * restricted to the ADMINISTRATOR-only controllers elsewhere.
 */
@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final AuthBridgeService authBridgeService;

    @GetMapping("/profile")
    public String viewProfile(Authentication authentication, Model model) {
        User user = authBridgeService.resolveCurrentUser(authentication);
        model.addAttribute("user", user);

        if (!model.containsAttribute("profileForm")) {
            ProfileForm form = new ProfileForm();
            form.setFullName(user.getFullName());
            form.setEmail(user.getEmail());
            model.addAttribute("profileForm", form);
        }
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordChangeForm());
        }
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication authentication,
                                 @Valid @ModelAttribute("profileForm") ProfileForm form,
                                 BindingResult result,
                                 HttpServletRequest request,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        User user = authBridgeService.resolveCurrentUser(authentication);

        if (result.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("passwordForm", new PasswordChangeForm());
            return "profile";
        }

        boolean emailChanged;
        try {
            emailChanged = userService.updateProfile(user, form);
        } catch (IllegalArgumentException ex) {
            result.rejectValue("email", "duplicate", ex.getMessage());
            model.addAttribute("user", user);
            model.addAttribute("passwordForm", new PasswordChangeForm());
            return "profile";
        }

        if (emailChanged) {
            // Email doubles as the login username, so the session's
            // authenticated principal is now stale - end it cleanly and
            // have them log back in with the new address rather than
            // limping along with a mismatched session.
            request.getSession().invalidate();
            redirectAttributes.addFlashAttribute("successMessage",
                    "Your email was updated. Please log in again with your new email address.");
            return "redirect:/login";
        }

        redirectAttributes.addFlashAttribute("profileSuccessMessage", "Profile updated successfully.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(Authentication authentication,
                                  @Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
                                  BindingResult result,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        User user = authBridgeService.resolveCurrentUser(authentication);

        if (!result.hasFieldErrors("newPassword") && !result.hasFieldErrors("confirmNewPassword")
                && !form.getNewPassword().equals(form.getConfirmNewPassword())) {
            result.rejectValue("confirmNewPassword", "mismatch", "New passwords do not match");
        }

        if (!result.hasErrors()) {
            try {
                userService.changePassword(user, form);
            } catch (IllegalArgumentException ex) {
                result.rejectValue("currentPassword", "invalid", ex.getMessage());
            }
        }

        if (result.hasErrors()) {
            model.addAttribute("user", user);
            ProfileForm profileForm = new ProfileForm();
            profileForm.setFullName(user.getFullName());
            profileForm.setEmail(user.getEmail());
            model.addAttribute("profileForm", profileForm);
            return "profile";
        }

        redirectAttributes.addFlashAttribute("profileSuccessMessage", "Password changed successfully.");
        return "redirect:/profile";
    }
}
