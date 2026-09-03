package com.laposte.adminmobile.service;

import com.laposte.adminmobile.entities.Support;
import com.laposte.adminmobile.repository.SupportRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Boot port of {@code admin.mobile.dao.supportdao}.
 */
@Service
public class SupportService {

    private final SupportRepository supportRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public SupportService(SupportRepository supportRepository) {
        this.supportRepository = supportRepository;
    }

    public List<Support> allSupport() {
        return supportRepository.findAll();
    }

    public Support find(BigDecimal id) {
        return supportRepository.findById(id).orElse(null);
    }

    @Transactional
    public Support create(Support a) {
        return supportRepository.save(a);
    }

    @Transactional
    public void delete(Support a) {
        supportRepository.delete(a);
    }

    @Transactional
    public Support edit(Support a) {
        return supportRepository.save(a);
    }

    @Transactional
    public void refresh(Support a) {
        entityManager.refresh(a);
    }

    public List<Support> getSupportenreg(String telephonsupport) {
        return supportRepository.findByTelephonsupport(telephonsupport);
    }
}
