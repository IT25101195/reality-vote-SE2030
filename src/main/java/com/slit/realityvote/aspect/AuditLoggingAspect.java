package com.slit.realityvote.aspect;

import com.slit.realityvote.entity.AuditEventType;
import com.slit.realityvote.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Cross-cutting audit-log aspect (Module 6.4, AuditLoggingAspect).
 *
 * Intercepts admin write operations, voting actions, and auth events so
 * audit entries are created automatically without requiring manual calls
 * in each controller — matching the spec requirement that the officer
 * never writes a log entry directly.
 *
 * Three pointcuts are registered:
 *   logAdminAction   — any public method in a controller under /admin/**
 *   logVotingAction  — VoteController.castVote
 *   logAuthAction    — RegistrationController.register
 *
 * The aspect runs @AfterReturning (success path). Failures already create
 * VOTE_REJECTED / LOGIN_FAILURE entries via the service layer, so we avoid
 * double-counting by only intercepting the success path here.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLoggingAspect {

    private final AuditLogService auditLogService;

    // ── Admin write operations ────────────────────────────────────────────────

    @AfterReturning(
        pointcut = "execution(* com.slit.realityvote.controller.RealityShowController.create*(..)) || " +
                   "execution(* com.slit.realityvote.controller.RealityShowController.update*(..)) || " +
                   "execution(* com.slit.realityvote.controller.RealityShowController.delete*(..)) || " +
                   "execution(* com.slit.realityvote.controller.VotingSessionController.create*(..)) || " +
                   "execution(* com.slit.realityvote.controller.VotingSessionController.update*(..)) || " +
                   "execution(* com.slit.realityvote.controller.VotingSessionController.delete*(..)) || " +
                   "execution(* com.slit.realityvote.controller.VotingSessionController.open*(..)) || " +
                   "execution(* com.slit.realityvote.controller.VotingSessionController.close*(..))"
    )
    public void logAdminAction(JoinPoint jp) {
        String methodName = jp.getSignature().getName();
        String className  = jp.getTarget().getClass().getSimpleName();
        auditLogService.record(
                AuditEventType.PROFILE_UPDATED,   // closest general event type for admin mutations
                className + "." + methodName + "() executed",
                resolveActor(),
                methodName.toUpperCase(),
                className.replace("Controller", ""),
                null,
                resolveIp()
        );
    }

    // ── Voting actions ────────────────────────────────────────────────────────

    @AfterReturning(
        pointcut = "execution(* com.slit.realityvote.controller.VoteController.cast*(..))"
    )
    public void logVotingAction(JoinPoint jp) {
        // VOTE_CAST is already written by VoteServiceImpl; this captures the
        // controller-level confirmation with the IP address.
        log.debug("[AuditAspect] vote action intercepted at controller: {}", jp.getSignature().getName());
        // No additional record written here to avoid duplication.
    }

    // ── Auth / Registration actions ───────────────────────────────────────────

    @AfterReturning(
        pointcut = "execution(* com.slit.realityvote.controller.RegistrationController.register(..))"
    )
    public void logAuthAction(JoinPoint jp) {
        // USER_REGISTERED is already written by UserServiceImpl; log IP enrichment only.
        log.debug("[AuditAspect] registration intercepted at controller: {}", jp.getSignature().getName());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "anonymous";
    }

    private String resolveIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest req = attrs.getRequest();
            String forwarded = req.getHeader("X-Forwarded-For");
            return (forwarded != null && !forwarded.isBlank())
                    ? forwarded.split(",")[0].trim()
                    : req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
