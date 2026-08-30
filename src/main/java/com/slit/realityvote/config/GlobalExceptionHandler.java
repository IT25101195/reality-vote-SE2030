package com.slit.realityvote.config;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Converts EntityNotFoundException (thrown by the service layer when an
 * id doesn't exist) into our own 404 page instead of Spring's default
 * Whitelabel Error Page - satisfies the "No white-label pages" /
 * "custom 404 page" requirement.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(EntityNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    // Catch-all safety net: without this, any exception we didn't anticipate
    // (bad/missing request params, DB constraint violations, etc.) falls
    // through to Spring Boot's default Whitelabel Error Page instead of our
    // own branded error screen.
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpected(Exception ex, Model model) {
        // Log the real cause to the console/log file - the page itself stays
        // generic on purpose (we don't want to leak stack traces to users),
        // but swallowing it entirely made bugs like this one hard to find.
        log.error("Unhandled exception", ex);
        model.addAttribute("message", "Something went wrong. Please try again.");
        return "error/404";
    }
}
