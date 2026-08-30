package com.slit.realityvote.config;

import com.slit.realityvote.entity.Judge;
import com.slit.realityvote.entity.JudgeStatus;
import com.slit.realityvote.entity.Role;
import com.slit.realityvote.entity.User;
import com.slit.realityvote.repository.JudgeRepository;
import com.slit.realityvote.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Login moved from InMemoryUserDetailsManager to a real `users` table
 * (see DatabaseUserDetailsService). The one non-voter account per role
 * that used to live in SecurityConfig now needs to exist as an actual
 * row, or nobody could log in as ADMINISTRATOR/STAFF/etc. on a fresh
 * database. Runs once at startup; skips any account that already exists,
 * so it's safe to leave in place permanently.
 *
 * These are staff/admin accounts, not self-registrations - the public
 * /register page (RegistrationController) only ever creates Role.VIEWER
 * accounts, matching "contestant management only handled by admin side".
 */
@Component
@RequiredArgsConstructor
public class DemoAccountSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final JudgeRepository judgeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seed("Administrator", "admin@realityvote.lk", "Admin@123", Role.ADMINISTRATOR);
        seed("Contestant Staff", "staff@realityvote.lk", "Staff@123", Role.CONTESTANT_STAFF);
        seed("Demo Viewer", "viewer@realityvote.lk", "Viewer@123", Role.VIEWER);
        seed("Compliance Officer", "compliance@realityvote.lk", "Compliance@123", Role.COMPLIANCE_OFFICER);
        seed("Reporting Manager", "reports@realityvote.lk", "Reports@123", Role.REPORTING_MANAGER);
        seed("Support Staff", "support@realityvote.lk", "Support@123", Role.SUPPORT_STAFF);
        seed("Demo Judge", "judge@realityvote.lk", "Judge@123", Role.JUDGE);
        seedJudgeProfile("Demo Judge", "judge@realityvote.lk");
    }

    // Sentinel password AuthBridgeServiceImpl uses when it auto-provisions a
    // `users` row for someone authenticated in-memory (old flow) rather than
    // a real password. If a demo account's row was created that way before
    // this seeder ever ran, "skip if exists" would leave it permanently
    // unable to log in - so repair that specific case instead of just
    // skipping.
    private static final String AUTH_BRIDGE_PLACEHOLDER = "N/A-managed-by-spring-security";

    private void seed(String fullName, String email, String rawPassword, Role role) {
        userRepository.findByEmail(email).ifPresentOrElse(existing -> {
            if (AUTH_BRIDGE_PLACEHOLDER.equals(existing.getPassword())) {
                existing.setPassword(passwordEncoder.encode(rawPassword));
                userRepository.save(existing);
            }
        }, () -> userRepository.save(User.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .enabled(true)
                .build()));
    }

    // The Demo Judge login account (above) needs a matching Judge domain
    // profile row - see the Judge entity javadoc for why the two are
    // separate - or ScoreController would have no assignments/history to
    // show them after logging in.
    private void seedJudgeProfile(String fullName, String email) {
        if (judgeRepository.existsByEmailIgnoreCaseAndDeletedFalse(email)) {
            return;
        }
        judgeRepository.save(Judge.builder()
                .fullName(fullName)
                .email(email)
                .phone("+94 77 000 0000")
                .expertiseArea("Vocal Coach")
                .bio("Demo judge profile seeded for evaluation purposes.")
                .status(JudgeStatus.ACTIVE)
                .deleted(false)
                .build());
    }
}
