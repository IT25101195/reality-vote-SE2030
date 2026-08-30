package com.slit.realityvote.controller;

import com.slit.realityvote.aspect.AuditLoggingAspect;
import com.slit.realityvote.config.SecurityConfig;
import com.slit.realityvote.entity.AuditEventType;
import com.slit.realityvote.entity.AuditLog;
import com.slit.realityvote.security.DatabaseUserDetailsService;
import com.slit.realityvote.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @WebMvcTest for AuditLogController.
 *
 * Imports the real SecurityConfig so role-based access rules are enforced
 * in the test environment — SUPPORT_STAFF must receive 403.
 */
@WebMvcTest(AuditLogController.class)
@Import(SecurityConfig.class)
class AuditLogControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean AuditLogService auditLogService;
    @MockBean DatabaseUserDetailsService userDetailsService;
    @MockBean AuditLoggingAspect auditLoggingAspect;

    private Page<AuditLog> emptyPage() {
        return new PageImpl<>(List.of());
    }

    @Test
    @WithMockUser(roles = "COMPLIANCE_OFFICER")
    void list_complianceOfficer_returns200() throws Exception {
        when(auditLogService.search(isNull(), anyBoolean(), isNull(), any()))
                .thenReturn(emptyPage());
        when(auditLogService.countFlagged()).thenReturn(0L);

        mockMvc.perform(get("/compliance/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(view().name("audit/list"));
    }

    @Test
    @WithMockUser(roles = "SUPPORT_STAFF")
    void list_supportStaff_isForbidden() throws Exception {
        // Hard constraint from T-16.3: SUPPORT_STAFF cannot access audit logs.
        mockMvc.perform(get("/compliance/audit-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COMPLIANCE_OFFICER")
    void detail_existingEntry_returns200() throws Exception {
        AuditLog log = AuditLog.builder()
                .id(1L)
                .eventType(AuditEventType.LOGIN_SUCCESS)
                .description("test")
                .actorEmail("a@b.com")
                .createdDate(LocalDateTime.now())
                // Leave action/entityType/entityId null so the Thymeleaf
                // th:if guards on those optional fields are exercised.
                .build();
        when(auditLogService.getById(1L)).thenReturn(Optional.of(log));

        mockMvc.perform(get("/compliance/audit-logs/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("audit/detail"));
    }

    @Test
    @WithMockUser(roles = "COMPLIANCE_OFFICER")
    void byEntity_returnsEntityHistory() throws Exception {
        when(auditLogService.getByEntity("Vote", 5L)).thenReturn(List.of());

        mockMvc.perform(get("/compliance/audit-logs/entity/Vote/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("audit/list-entity"));
    }
}
