package com.slit.realityvote.service;

import com.slit.realityvote.entity.SupportTicket;
import com.slit.realityvote.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SupportTicketService {

    SupportTicket submitTicket(String subject, String description, Long submitterId);

    List<SupportTicket> getTicketsForUser(Long userId);

    SupportTicket getById(Long id);

    Page<SupportTicket> searchForStaff(TicketStatus status, Pageable pageable);

    SupportTicket respond(Long ticketId, String response, TicketStatus newStatus, Long staffUserId);

    long countByStatus(TicketStatus status);
}
