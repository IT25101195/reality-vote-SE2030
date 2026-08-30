package com.slit.realityvote.service.impl;

import com.slit.realityvote.entity.Contestant;
import com.slit.realityvote.entity.ContestantStatus;
import com.slit.realityvote.entity.RealityShow;
import com.slit.realityvote.entity.Season;
import com.slit.realityvote.repository.ContestantRepository;
import com.slit.realityvote.repository.RealityShowRepository;
import com.slit.realityvote.repository.SeasonRepository;
import com.slit.realityvote.service.ContestantService;
import com.slit.realityvote.service.FileStorageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ContestantServiceImpl implements ContestantService {

    private final ContestantRepository contestantRepository;
    private final RealityShowRepository showRepository;
    private final SeasonRepository seasonRepository;
    private final FileStorageService fileStorageService;

    @Override
    public Page<Contestant> search(String keyword, Long showId, ContestantStatus status, Pageable pageable) {
        String cleanKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return contestantRepository.search(cleanKeyword, showId, status, pageable);
    }

    @Override
    public Contestant getById(Long id) {
        return contestantRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Contestant not found with id: " + id));
    }

    @Override
    @Transactional
    public Contestant createContestant(Contestant contestant, Long showId, Long seasonId, MultipartFile photo) {
        RealityShow show = showRepository.findByIdAndDeletedFalse(showId)
                .orElseThrow(() -> new EntityNotFoundException("Reality show not found with id: " + showId));

        // Duplicate detection (requirements doc requirement #8)
        if (contestantRepository.existsByFullNameIgnoreCaseAndShow_IdAndDeletedFalse(contestant.getFullName(), showId)) {
            throw new IllegalArgumentException(
                    "A contestant named '" + contestant.getFullName() + "' is already registered for this show.");
        }

        contestant.setShow(show);
        assignSeasonIfProvided(contestant, seasonId);
        contestant.setStatus(ContestantStatus.ACTIVE);
        contestant.setDeleted(false);

        if (photo != null && !photo.isEmpty()) {
            contestant.setPhotoPath(fileStorageService.storeContestantPhoto(photo));
        }

        return contestantRepository.save(contestant);
    }

    @Override
    @Transactional
    public Contestant updateContestant(Long id, Contestant updated, Long showId, Long seasonId, MultipartFile photo) {
        Contestant existing = getById(id);

        RealityShow show = showRepository.findByIdAndDeletedFalse(showId)
                .orElseThrow(() -> new EntityNotFoundException("Reality show not found with id: " + showId));

        // Only enforce the duplicate check if the name or show actually changed
        boolean nameOrShowChanged = !existing.getFullName().equalsIgnoreCase(updated.getFullName())
                || !existing.getShow().getId().equals(showId);
        if (nameOrShowChanged &&
                contestantRepository.existsByFullNameIgnoreCaseAndShow_IdAndDeletedFalse(updated.getFullName(), showId)) {
            throw new IllegalArgumentException(
                    "A contestant named '" + updated.getFullName() + "' is already registered for this show.");
        }

        existing.setFullName(updated.getFullName());
        existing.setAge(updated.getAge());
        existing.setHometown(updated.getHometown());
        existing.setTalentCategory(updated.getTalentCategory());
        existing.setBiography(updated.getBiography());
        existing.setShow(show);
        assignSeasonIfProvided(existing, seasonId);

        if (photo != null && !photo.isEmpty()) {
            existing.setPhotoPath(fileStorageService.storeContestantPhoto(photo));
        }

        return contestantRepository.save(existing);
    }

    @Override
    @Transactional
    public void deactivateContestant(Long id) {
        Contestant contestant = getById(id);
        // Soft delete: keeps voting history / rankings intact for reports,
        // matching the "Remove/Deactivate Contestant" requirement rather
        // than a hard DB delete.
        contestant.setDeleted(true);
        contestant.setStatus(ContestantStatus.WITHDRAWN);
        contestantRepository.save(contestant);
    }

    @Override
    @Transactional
    public Contestant updateStatus(Long id, ContestantStatus newStatus) {
        Contestant contestant = getById(id);
        contestant.setStatus(newStatus);
        return contestantRepository.save(contestant);
    }

    private void assignSeasonIfProvided(Contestant contestant, Long seasonId) {
        if (seasonId == null) {
            contestant.setSeason(null);
            return;
        }
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new EntityNotFoundException("Season not found with id: " + seasonId));
        contestant.setSeason(season);
    }
}
