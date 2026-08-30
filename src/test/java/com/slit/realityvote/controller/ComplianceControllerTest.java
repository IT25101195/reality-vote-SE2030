package com.slit.realityvote.controller;

import com.slit.realityvote.aspect.AuditLoggingAspect;
import com.slit.realityvote.config.SecurityConfig;
import com.slit.realityvote.dto.ComplianceDashboardStats;
import com.slit.realityvote.security.DatabaseUserDetailsService;
import com.slit.realityvote.service.AuditLogService;
import com.slit.realityvote.service.ComplianceService;
import com.slit.realityvote.service.ComplianceReportRecordService;
import com.slit.realityvote.service.FraudReportService;
import com.slit.realityvote.repository.VotingSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * @WebMvcTest for ComplianceController.
 *
 * @Import(SecurityConfig.class) loads the real security rules so the
 * forbidden-role tests get genuine 403 responses rather than the
 * default auto-configured permissive behaviour.
 *
 * Covers PBI-16 separation-of-duties enforcement: VIEWER is rejected
 * at every compliance endpoint.
 */
@WebMvcTest(ComplianceController.class)
@Import(SecurityConfig.class)
class ComplianceControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean ComplianceService complianceService;
    @MockBean AuditLogService auditLogService;
    @MockBean FraudReportService fraudReportService;
    @MockBean ComplianceReportRecordService complianceReportRecordService;
    @MockBean VotingSessionRepository sessionRepository;
    @MockBean DatabaseUserDetailsService userDetailsService;
    @MockBean AuditLoggingAspect auditLoggingAspect;

    @Test
    @WithMockUser(roles = "COMPLIANCE_OFFICER")
    void dashboard_complianceOfficer_returns200() throws Exception {
        when(complianceService.getDashboardStats())
                .thenReturn(new ComplianceDashboardStats(100, 5, 2, 10, 3, 1));
        when(auditLogService.getTopFlaggedActors(5)).thenReturn(List.of());
        when(sessionRepository.findAllByOrderByStartTimeDesc()).thenReturn(List.of());

        mockMvc.perform(get("/compliance/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("compliance/dashboard"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void dashboard_administrator_returns200() throws Exception {
        when(complianceService.getDashboardStats())
                .thenReturn(new ComplianceDashboardStats(0, 0, 0, 0, 0, 0));
        when(auditLogService.getTopFlaggedActors(5)).thenReturn(List.of());
        when(sessionRepository.findAllByOrderByStartTimeDesc()).thenReturn(List.of());

        mockMvc.perform(get("/compliance/dashboard"))
                .andExpect(status().isOk());
    }

    // ── Separation-of-duties enforcement (Task T-16.3) ─────────────────────

    @Test
    @WithMockUser(roles = "VIEWER")
    void dashboard_viewer_isForbidden() throws Exception {
        // VIEWER must be blocked — Spring Security should return 403 before
        // the controller method is even invoked.
        mockMvc.perform(get("/compliance/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void activity_viewer_isForbidden() throws Exception {
        mockMvc.perform(get("/compliance/activity"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void anomalies_viewer_isForbidden() throws Exception {
        mockMvc.perform(get("/compliance/anomalies"))
                .andExpect(status().isForbidden());
    }
}
