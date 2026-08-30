package com.slit.realityvote.repository;

import com.slit.realityvote.entity.SupportTicket;
import com.slit.realityvote.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findBySubmittedBy_IdOrderByCreatedDateDesc(Long submitterId);

    @Query("SELECT t FROM SupportTicket t WHERE " +
           "(:status IS NULL OR t.status = :status) " +
           "ORDER BY t.createdDate DESC")
    Page<SupportTicket> search(@Param("status") TicketStatus status, Pageable pageable);

    long countByStatus(TicketStatus status);
}
