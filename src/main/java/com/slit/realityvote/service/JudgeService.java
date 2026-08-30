package com.slit.realityvote.service;

import com.slit.realityvote.entity.Judge;
import com.slit.realityvote.entity.JudgeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface JudgeService {

    Page<Judge> search(String keyword, JudgeStatus status, Pageable pageable);

    Judge getById(Long id);

    Judge createJudge(Judge judge, MultipartFile photo);

    Judge updateJudge(Long id, Judge updated, MultipartFile photo);

    void deactivateJudge(Long id); // soft delete

    Judge updateStatus(Long id, JudgeStatus newStatus);
}