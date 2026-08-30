package com.slit.realityvote.security;

import com.slit.realityvote.entity.User;
import com.slit.realityvote.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Real UserDetailsService backed by the `users` table - this is the class
 * SecurityConfig's old comment pointed to ("swap the userDetailsService()
 * bean for a DB-backed one once that module is ready"). Every account,
 * voter or staff, now authenticates against UserRepository instead of the
 * old InMemoryUserDetailsManager.
 */
@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("No account found for " + email));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .roles(user.getRole().name())
                .build();
    }
}
