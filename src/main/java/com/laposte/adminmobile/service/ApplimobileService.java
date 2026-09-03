package com.laposte.adminmobile.service;

import com.laposte.adminmobile.entities.Applimobile;
import com.laposte.adminmobile.repository.ApplimobileRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Boot port of {@code admin.mobile.dao.applidao}.
 */
@Service
public class ApplimobileService {

    private final ApplimobileRepository applimobileRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public ApplimobileService(ApplimobileRepository applimobileRepository) {
        this.applimobileRepository = applimobileRepository;
    }

    public List<Applimobile> allApplication() {
        return applimobileRepository.findAll();
    }

    /** Kept alongside allApplication() - the legacy DAO exposed both under the same query. */
    public List<Applimobile> allp() {
        return allApplication();
    }

    public Applimobile find(BigDecimal id) {
        return applimobileRepository.findById(id).orElse(null);
    }

    @Transactional
    public Applimobile create(Applimobile a) {
        return applimobileRepository.save(a);
    }

    @Transactional
    public void delete(Applimobile a) {
        applimobileRepository.delete(a);
    }

    @Transactional
    public Applimobile edit(Applimobile a) {
        return applimobileRepository.save(a);
    }

    @Transactional
    public void refresh(Applimobile a) {
        entityManager.refresh(a);
    }
}
