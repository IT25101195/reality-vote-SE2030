
package com.slit.realityvote.service.impl;

import com.slit.realityvote.entity.Judge;
import com.slit.realityvote.entity.JudgeStatus;
import com.slit.realityvote.repository.JudgeRepository;
import com.slit.realityvote.service.FileStorageService;
import com.slit.realityvote.service.JudgeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {

    private final JudgeRepository judgeRepository;
    private final FileStorageService fileStorageService;

    @Override
    public Page<Judge> search(String keyword, JudgeStatus status, Pageable pageable) {
        return judgeRepository.search(keyword, status, pageable);
    }

    @Override
    public Judge getById(Long id) {
        return judgeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Judge not found with id: " + id));
    }

    @Override
    @Transactional
    public Judge createJudge(Judge judge, MultipartFile photo) {
        // Duplicate-detection, same pattern as ContestantServiceImpl: a
        // judge's email doubles as their scoring-login identity, so it
        // must be unique across active judge profiles.
        if (judgeRepository.existsByEmailIgnoreCaseAndDeletedFalse(judge.getEmail())) {
            throw new IllegalArgumentException("A judge with this email is already registered.");
        }
        judge.setId(null);
        judge.setDeleted(false);
        if (photo != null && !photo.isEmpty()) {
            judge.setPhotoPath(fileStorageService.storeJudgePhoto(photo));
        }
        return judgeRepository.save(judge);
    }

    @Override
    @Transactional
    public Judge updateJudge(Long id, Judge updated, MultipartFile photo) {
        Judge existing = getById(id);

        judgeRepository.findByEmailIgnoreCaseAndDeletedFalse(updated.getEmail())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("Another judge already uses this email.");
                });

        existing.setFullName(updated.getFullName());
        existing.setEmail(updated.getEmail());
        existing.setPhone(updated.getPhone());
        existing.setExpertiseArea(updated.getExpertiseArea());
        existing.setBio(updated.getBio());
        if (photo != null && !photo.isEmpty()) {
            existing.setPhotoPath(fileStorageService.storeJudgePhoto(photo));
        }
        return judgeRepository.save(existing);
    }

    @Override
    @Transactional
    public void deactivateJudge(Long id) {
        Judge judge = getById(id);
        judge.setDeleted(true);
        judge.setStatus(JudgeStatus.INACTIVE);
        judgeRepository.save(judge);
    }

    @Override
    @Transactional
    public Judge updateStatus(Long id, JudgeStatus newStatus) {
        Judge judge = getById(id);
        judge.setStatus(newStatus);
        return judgeRepository.save(judge);
    }
}