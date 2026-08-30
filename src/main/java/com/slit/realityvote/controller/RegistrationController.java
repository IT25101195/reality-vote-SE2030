package com.slit.realityvote.controller;

import com.slit.realityvote.dto.RegistrationForm;
import com.slit.realityvote.entity.User;
import com.slit.realityvote.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Public voter sign-up. Anyone can create an account here, but it can
 * only ever result in a VIEWER (voter) account - see UserServiceImpl.
 * Contestants and staff/admin accounts are never created through this
 * form; contestants have no login at all (see ContestantController /
 * Contestant entity - they're managed records, not accounts).
 */
@Controller
@RequiredArgsConstructor
public class RegistrationController {

    private final UserService userService;

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                            BindingResult result,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {

        if (!result.hasFieldErrors("password") && !result.hasFieldErrors("confirmPassword")
                && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        if (!result.hasFieldErrors("email") && userService.emailTaken(form.getEmail())) {
            result.rejectValue("email", "duplicate", "An account with this email already exists");
        }

        if (result.hasErrors()) {
            return "register";
        }

        User created;
        try {
            created = userService.registerViewer(form);
        } catch (IllegalArgumentException ex) {
            // Covers the rare race where two submissions for the same email
            // both slipped past the emailTaken() check above (e.g. a
            // double-click) - shown as a normal field error instead of
            // crashing out to the generic error page.
            result.rejectValue("email", "duplicate", ex.getMessage());
            return "register";
        }

        signInImmediately(created, request);
        redirectAttributes.addFlashAttribute("successMessage",
                "Welcome, " + created.getFullName() + "! Your account is ready - here's what's open for voting.");
        return "redirect:/vote";
    }

    // Registration already proved who this person is (they just chose the
    // password themselves), so sign them straight in rather than making
    // them retype credentials on the login page immediately afterward.
    private void signInImmediately(User user, HttpServletRequest request) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Persist into the session using the same attribute key Spring
        // Security's own session-backed SecurityContextRepository reads on
        // the next request, so the user is genuinely logged in - not just
        // authenticated for the rest of this one request.
        request.getSession(true)
                .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }
}
