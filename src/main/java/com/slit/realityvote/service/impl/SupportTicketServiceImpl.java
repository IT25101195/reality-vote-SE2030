package com.slit.realityvote.service.impl;

import com.slit.realityvote.entity.SupportTicket;
import com.slit.realityvote.entity.TicketStatus;
import com.slit.realityvote.entity.User;
import com.slit.realityvote.repository.SupportTicketRepository;
import com.slit.realityvote.repository.UserRepository;
import com.slit.realityvote.service.SupportTicketService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SupportTicket submitTicket(String subject, String description, Long submitterId) {
        User submitter = userRepository.findById(submitterId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + submitterId));

        SupportTicket ticket = SupportTicket.builder()
                .subject(subject)
                .description(description)
                .submittedBy(submitter)
                .status(TicketStatus.OPEN)
                .build();
        // The reference number (TCK-000042) is derived from the id, so it
        // only exists after this save assigns one.
        return ticketRepository.save(ticket);
    }

    @Override
    public List<SupportTicket> getTicketsForUser(Long userId) {
        return ticketRepository.findBySubmittedBy_IdOrderByCreatedDateDesc(userId);
    }

    @Override
    public SupportTicket getById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Support ticket not found with id: " + id));
    }

    @Override
    public Page<SupportTicket> searchForStaff(TicketStatus status, Pageable pageable) {
        return ticketRepository.search(status, pageable);
    }

    @Override
    @Transactional
    public SupportTicket respond(Long ticketId, String response, TicketStatus newStatus, Long staffUserId) {
        SupportTicket ticket = getById(ticketId);
        User staff = userRepository.findById(staffUserId)
                .orElseThrow(() -> new EntityNotFoundException("Staff user not found with id: " + staffUserId));

        ticket.setStaffResponse(response);
        ticket.setStatus(newStatus);
        ticket.setResolvedBy(staff);
        return ticketRepository.save(ticket);
    }

    @Override
    public long countByStatus(TicketStatus status) {
        return ticketRepository.countByStatus(status);
    }
}
