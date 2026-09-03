package com.laposte.adminmobile.service;

import com.laposte.adminmobile.entities.Suggestions;
import com.laposte.adminmobile.repository.SuggestionsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Boot port of {@code admin.mobile.dao.sugestiondao}.
 */
@Service
public class SuggestionsService {

    private final SuggestionsRepository suggestionsRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public SuggestionsService(SuggestionsRepository suggestionsRepository) {
        this.suggestionsRepository = suggestionsRepository;
    }

    public List<Suggestions> allSugestion() {
        return suggestionsRepository.findAll();
    }

    public Suggestions find(BigDecimal id) {
        return suggestionsRepository.findById(id).orElse(null);
    }

    @Transactional
    public Suggestions create(Suggestions a) {
        return suggestionsRepository.save(a);
    }

    @Transactional
    public void delete(Suggestions a) {
        suggestionsRepository.delete(a);
    }

    @Transactional
    public Suggestions edit(Suggestions a) {
        return suggestionsRepository.save(a);
    }

    @Transactional
    public void refresh(Suggestions a) {
        entityManager.refresh(a);
    }

    /** Port of {@code listesugest.xhtml}: suggestions in a date range, most recent first. */
    public List<Suggestions> findByPeriode(Date date1, Date date2) {
        return suggestionsRepository.findByDatesugestBetweenOrderByDatesugestDesc(date1, date2);
    }

    /** All suggestions, most recent first - used for the /suggestions screen's recap list. */
    public List<Suggestions> allSugestionRecent() {
        return suggestionsRepository.findAllByOrderByDatesugestDesc();
    }
}
