package com.slit.realityvote.service.impl;

import com.slit.realityvote.entity.Role;
import com.slit.realityvote.entity.User;
import com.slit.realityvote.repository.UserRepository;
import com.slit.realityvote.service.AuthBridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * NOTE FOR THE TEAM: SecurityConfig currently defines demo accounts with
 * InMemoryUserDetailsManager rather than reading from the `users` table
 * (that's the Registration/Login module's job, not built yet). But other
 * modules - Voting, Support Tickets - need a real User row to attach a
 * foreign key to (Vote.voter, SupportTicket.submittedBy). This class is
 * the ONE place that bridges the two: given whoever Spring Security says
 * is logged in, find or create their `users` row.
 *
 * Once real DB-backed registration exists, delete this class and have
 * SecurityConfig's UserDetailsService read directly from UserRepository -
 * every controller that depends on AuthBridgeService can stay unchanged,
 * since they only ever ask for "the current user," not how that's resolved.
 */
@Service
@RequiredArgsConstructor
public class AuthBridgeServiceImpl implements AuthBridgeService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public User resolveCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(User.builder()
                        .fullName(email)
                        .email(email)
                        .password("N/A-managed-by-spring-security")
                        .role(inferRole(authentication))
                        .enabled(true)
                        .build()));
    }

    private Role inferRole(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String name = authority.getAuthority().replace("ROLE_", "");
            try {
                return Role.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // not one of our Role enum values - skip it
            }
        }
        return Role.VIEWER; // safe default
    }
}
