package com.slit.realityvote.service.impl;

import com.slit.realityvote.dto.PasswordChangeForm;
import com.slit.realityvote.dto.ProfileForm;
import com.slit.realityvote.dto.RegistrationForm;
import com.slit.realityvote.entity.AuditEventType;
import com.slit.realityvote.entity.Role;
import com.slit.realityvote.entity.User;
import com.slit.realityvote.repository.UserRepository;
import com.slit.realityvote.service.AuditLogService;
import com.slit.realityvote.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Override
    public boolean emailTaken(String email) {
        return userRepository.findByEmail(normalize(email)).isPresent();
    }

    @Override
    @Transactional
    public User registerViewer(RegistrationForm form) {
        String email = normalize(form.getEmail());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        User user = User.builder()
                .fullName(form.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(form.getPassword()))
                .role(Role.VIEWER) // self-registration can only ever create voters
                .enabled(true)
                .build();

        User saved;
        try {
            saved = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Two submissions for the same email landed at (almost) the same
            // time and both passed the findByEmail check above before either
            // committed - the unique constraint on users.email is what
            // actually catches it. Surface it the same way as the normal
            // duplicate-email case instead of a raw SQL error.
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        auditLogService.record(AuditEventType.USER_REGISTERED, "New voter account registered", email);
        return saved;
    }

    @Override
    @Transactional
    public User createStaffAccount(String fullName, String email, String rawPassword, Role role) {
        String normalizedEmail = normalize(email);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        User user = User.builder()
                .fullName(fullName.trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .enabled(true)
                .build();

        User saved;
        try {
            saved = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        auditLogService.record(AuditEventType.USER_REGISTERED,
                role + " login account created by administrator", normalizedEmail);
        return saved;
    }

    @Override
    @Transactional
    public boolean updateProfile(User user, ProfileForm form) {
        String newEmail = normalize(form.getEmail());
        boolean emailChanged = !newEmail.equals(user.getEmail());

        if (emailChanged && userRepository.findByEmail(newEmail).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        user.setFullName(form.getFullName().trim());
        user.setEmail(newEmail);
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        auditLogService.record(AuditEventType.PROFILE_UPDATED,
                emailChanged ? "Profile updated (email changed)" : "Profile updated",
                newEmail);
        return emailChanged;
    }

    @Override
    @Transactional
    public void changePassword(User user, PasswordChangeForm form) {
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        user.setPassword(passwordEncoder.encode(form.getNewPassword()));
        userRepository.save(user);
        auditLogService.record(AuditEventType.PASSWORD_CHANGED, "User changed their password", user.getEmail());
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
