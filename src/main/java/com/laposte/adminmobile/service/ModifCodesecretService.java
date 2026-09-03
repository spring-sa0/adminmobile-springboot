package com.laposte.adminmobile.service;

import com.laposte.adminmobile.entities.ModifCodesecret;
import com.laposte.adminmobile.repository.ModifCodesecretRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Boot port of {@code admin.mobile.dao.modifsecretdao2}.
 */
@Service
public class ModifCodesecretService {

    private final ModifCodesecretRepository modifCodesecretRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public ModifCodesecretService(ModifCodesecretRepository modifCodesecretRepository) {
        this.modifCodesecretRepository = modifCodesecretRepository;
    }

    public List<ModifCodesecret> allmodifcode() {
        return modifCodesecretRepository.findAll();
    }

    /** Kept alongside allmodifcode() - the legacy DAO exposed both under the same query. */
    public List<ModifCodesecret> allcode() {
        return allmodifcode();
    }

    public ModifCodesecret find(BigDecimal id) {
        return modifCodesecretRepository.findById(id).orElse(null);
    }

    @Transactional
    public ModifCodesecret create(ModifCodesecret a) {
        return modifCodesecretRepository.save(a);
    }

    @Transactional
    public void delete(ModifCodesecret a) {
        modifCodesecretRepository.delete(a);
    }

    @Transactional
    public ModifCodesecret edit(ModifCodesecret a) {
        return modifCodesecretRepository.save(a);
    }

    @Transactional
    public void refresh(ModifCodesecret a) {
        entityManager.refresh(a);
    }

    /** Port of {@code etatmodifpwd.xhtml}: password/code-secret changes in a date range. */
    public List<ModifCodesecret> findByPeriode(Date date1, Date date2) {
        return modifCodesecretRepository.findByDateetheureBetweenOrderByDateetheureDesc(date1, date2);
    }
}
