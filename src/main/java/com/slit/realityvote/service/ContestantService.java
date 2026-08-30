package com.slit.realityvote.service;

import com.slit.realityvote.entity.Contestant;
import com.slit.realityvote.entity.ContestantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ContestantService {

    Page<Contestant> search(String keyword, Long showId, ContestantStatus status, Pageable pageable);

    Contestant getById(Long id);

    Contestant createContestant(Contestant contestant, Long showId, Long seasonId, MultipartFile photo);

    Contestant updateContestant(Long id, Contestant updated, Long showId, Long seasonId, MultipartFile photo);

    void deactivateContestant(Long id); // soft delete

    Contestant updateStatus(Long id, ContestantStatus newStatus);
}
