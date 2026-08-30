package com.slit.realityvote.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Backing object for GET/POST /register.
 *
 * Deliberately NOT the User entity itself: binding straight to User would
 * let a malicious form post set fields like "role=ADMINISTRATOR" or
 * "enabled=true/false" (a classic mass-assignment bug). This form only
 * exposes what a self-registering voter should ever be able to set - the
 * service layer is the only place that decides role (always VIEWER).
 */
@Getter
@Setter
public class RegistrationForm {

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name is too long")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;
}
