package com.laposte.adminmobile.service;

import com.laposte.adminmobile.entities.Reclamations;
import com.laposte.adminmobile.repository.ReclamationsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Boot port of {@code admin.mobile.dao.reclamationdao}.
 */
@Service
public class ReclamationsService {

    private final ReclamationsRepository reclamationsRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public ReclamationsService(ReclamationsRepository reclamationsRepository) {
        this.reclamationsRepository = reclamationsRepository;
    }

    public List<Reclamations> allReclamation() {
        return reclamationsRepository.findAll();
    }

    public Reclamations find(BigDecimal id) {
        return reclamationsRepository.findById(id).orElse(null);
    }

    @Transactional
    public Reclamations create(Reclamations a) {
        return reclamationsRepository.save(a);
    }

    @Transactional
    public void delete(Reclamations a) {
        reclamationsRepository.delete(a);
    }

    @Transactional
    public Reclamations edit(Reclamations a) {
        return reclamationsRepository.save(a);
    }

    @Transactional
    public void refresh(Reclamations a) {
        entityManager.refresh(a);
    }

    public List<Reclamations> getReclamationNR() {
        return reclamationsRepository.findNonResolues();
    }

    public List<Reclamations> getReclaBySupport(String codeutilisateur) {
        return reclamationsRepository.findAffecteesByCodeutilisateur(codeutilisateur);
    }

    /** See {@link ReclamationsRepository#findForReport(String, Date, Date)}. */
    public List<Reclamations> getReportList(String etat, Date debut, Date fin) {
        return reclamationsRepository.findForReport(etat, debut, fin);
    }
}
