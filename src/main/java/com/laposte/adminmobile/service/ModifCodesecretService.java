package com.laposte.adminmobile.service;

import com.laposte.adminmobile.entities.ModifCodesecret;
import com.laposte.adminmobile.repository.ModifCodesecretRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Boot port of {@code admin.mobile.dao.modifsecretdao2}.
 * <p>
 * Les methodes find/edit/delete/refresh/allmodifcode du DAO d'origine ne sont pas portees :
 * elles reposent sur l'identite TELEPHONE, qui n'est pas unique dans ce journal (voir
 * {@link ModifCodesecret}), et aucun ecran ne les utilise.
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

    /**
     * Ajoute une ligne d'audit. save() ferait un merge(), donc un SELECT par telephone qui
     * plante ("Duplicate row was found and ASSERT was specified") des la 2e ligne pour un
     * meme numero ; persist() fait un INSERT direct, comme le DAO d'origine.
     */
    @Transactional
    public ModifCodesecret create(ModifCodesecret a) {
        entityManager.persist(a);
        return a;
    }

    /** Port of {@code etatmodifpwd.xhtml}: password/code-secret changes in a date range. */
    public List<ModifCodesecret> findByPeriode(Date date1, Date date2) {
        return modifCodesecretRepository.findByDateetheureBetweenOrderByDateetheureDesc(date1, date2);
    }
}
