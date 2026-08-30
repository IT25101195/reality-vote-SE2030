package com.slit.realityvote.repository;

import com.slit.realityvote.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {
    List<Faq> findAllByOrderByCategoryAsc();
}
