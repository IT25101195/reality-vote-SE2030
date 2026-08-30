package com.slit.realityvote.security;

import com.slit.realityvote.entity.AuditEventType;
import com.slit.realityvote.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Spring Security publishes AuthenticationSuccessEvent /
 * AbstractAuthenticationFailureEvent automatically on every login
 * attempt. We just listen and forward to AuditLogService, which is
 * also where the "3 failures in 10 minutes" fraud-detection rule fires -
 * satisfies "Login Logs" and "repeated failed login attempts" monitoring
 * from the requirements doc, without touching SecurityConfig itself.
 */
@Component
@RequiredArgsConstructor
public class AuthenticationEventListener {

    private final AuditLogService auditLogService;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();
        auditLogService.record(AuditEventType.LOGIN_SUCCESS, "Successful login", email);
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String email = event.getAuthentication().getName();
        auditLogService.record(AuditEventType.LOGIN_FAILURE,
                "Failed login attempt: " + event.getException().getMessage(), email);
    }
}
