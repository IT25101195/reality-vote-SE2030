package com.slit.realityvote.service.impl;

import com.slit.realityvote.entity.Faq;
import com.slit.realityvote.repository.FaqRepository;
import com.slit.realityvote.service.FaqService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FaqServiceImpl implements FaqService {

    private final FaqRepository faqRepository;

    @Override
    public List<Faq> getAll() {
        return faqRepository.findAllByOrderByCategoryAsc();
    }

    @Override
    public Faq getById(Long id) {
        return faqRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FAQ not found with id: " + id));
    }

    @Override
    @Transactional
    public Faq create(Faq faq) {
        return faqRepository.save(faq);
    }

    @Override
    @Transactional
    public Faq update(Long id, Faq updated) {
        Faq existing = getById(id);
        existing.setQuestion(updated.getQuestion());
        existing.setAnswer(updated.getAnswer());
        existing.setCategory(updated.getCategory());
        return faqRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        faqRepository.delete(getById(id));
    }
}
