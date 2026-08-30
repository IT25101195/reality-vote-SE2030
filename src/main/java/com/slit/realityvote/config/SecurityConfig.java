package com.slit.realityvote.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for this module.
 *
 * Login is DB-backed: Spring Security picks up the DatabaseUserDetailsService
 * bean (see security/DatabaseUserDetailsService.java) automatically since
 * it's the only UserDetailsService in the context, and authenticates
 * against the `users` table. Voters create their own row via the public
 * /register page (RegistrationController, always Role.VIEWER); the
 * staff/admin demo accounts are inserted once at startup by
 * config/DemoAccountSeeder.java so the same demo credentials keep working.
 *
 * @EnableMethodSecurity — enables @PreAuthorize in controllers (Module 6.4).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt satisfies the "Password Encryption" requirement.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/img/**",
                                 "/login", "/register", "/error/403", "/faq").permitAll()
                // Role-Based Access Control: only ADMINISTRATOR can manage shows
                .requestMatchers("/admin/shows/**").hasRole("ADMINISTRATOR")
                .requestMatchers("/admin/voting-sessions/**").hasRole("ADMINISTRATOR")
                .requestMatchers("/admin/judges/**").hasRole("ADMINISTRATOR")
                .requestMatchers("/staff/contestants/**").hasAnyRole("ADMINISTRATOR", "CONTESTANT_STAFF")
                .requestMatchers("/vote/**").hasRole("VIEWER")
                .requestMatchers("/judge/**").hasRole("JUDGE")
                // Module 6.4: entire /compliance/** tree is guarded here;
                // finer-grained @PreAuthorize annotations in controllers add
                // the per-endpoint ADMINISTRATOR-only constraints (e.g. purge).
                .requestMatchers("/compliance/**").hasAnyRole("ADMINISTRATOR", "COMPLIANCE_OFFICER")
                .requestMatchers("/reports/**").hasAnyRole("ADMINISTRATOR", "REPORTING_MANAGER")
                .requestMatchers("/staff/support/**", "/staff/faqs/**").hasAnyRole("ADMINISTRATOR", "SUPPORT_STAFF")
                .requestMatchers("/support/tickets/**").hasRole("VIEWER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .exceptionHandling(ex -> ex.accessDeniedPage("/error/403"))
            .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
            // CSRF protection is ON by default (Spring Security) - Thymeleaf
            // forms automatically include the token via th:action, satisfying
            // the "CSRF Protection" requirement.
            .csrf(csrf -> csrf.csrfTokenRepository(
                    org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse()));

        return http.build();
    }
}

