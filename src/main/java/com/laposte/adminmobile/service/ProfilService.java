package com.laposte.adminmobile.service;

import com.laposte.adminmobile.entities.Profil;
import com.laposte.adminmobile.repository.ProfilRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Boot port of {@code admin.mobile.dao.profildao}.
 */
@Service
public class ProfilService {

    private final ProfilRepository profilRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public ProfilService(ProfilRepository profilRepository) {
        this.profilRepository = profilRepository;
    }

    public List<Profil> allProfil() {
        return profilRepository.findAll();
    }

    /** Kept alongside allProfil() - the legacy DAO exposed both under the same query. */
    public List<Profil> allp() {
        return allProfil();
    }

    public Profil find(BigDecimal id) {
        return profilRepository.findById(id).orElse(null);
    }

    @Transactional
    public Profil create(Profil a) {
        return profilRepository.save(a);
    }

    @Transactional
    public void delete(Profil a) {
        profilRepository.delete(a);
    }

    @Transactional
    public Profil edit(Profil a) {
        return profilRepository.save(a);
    }

    @Transactional
    public void refresh(Profil a) {
        entityManager.refresh(a);
    }
}
