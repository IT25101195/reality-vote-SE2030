package com.slit.realityvote.service;

import com.slit.realityvote.entity.Faq;

import java.util.List;

public interface FaqService {
    List<Faq> getAll();
    Faq getById(Long id);
    Faq create(Faq faq);
    Faq update(Long id, Faq updated);
    void delete(Long id);
}
