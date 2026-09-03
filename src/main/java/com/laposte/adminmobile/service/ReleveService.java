package com.laposte.adminmobile.service;

import com.laposte.adminmobile.entities.Releve;
import com.laposte.adminmobile.repository.ReleveRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Boot port of {@code admin.mobile.dao.releveDAo}.
 */
@Service
public class ReleveService {

    private final ReleveRepository releveRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public ReleveService(ReleveRepository releveRepository) {
        this.releveRepository = releveRepository;
    }

    public List<Releve> allReleve() {
        return releveRepository.findAllOrderByIdreleveDesc();
    }

    public Releve find(Integer id) {
        return releveRepository.findById(id).orElse(null);
    }

    @Transactional
    public Releve create(Releve a) {
        return releveRepository.save(a);
    }

    @Transactional
    public void delete(Releve a) {
        releveRepository.delete(a);
    }

    @Transactional
    public Releve edit(Releve a) {
        return releveRepository.save(a);
    }

    @Transactional
    public void refresh(Releve a) {
        entityManager.refresh(a);
    }

    public List<Releve> getReleveEnreg(Long numerocompte, String jourenvoi, String mail, String typefichier) {
        return releveRepository.findByNumerocompteAndJourenvoiAndMailAndTypefichier(
                numerocompte, jourenvoi, mail, typefichier);
    }
}
