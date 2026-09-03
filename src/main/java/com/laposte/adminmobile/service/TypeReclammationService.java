package com.laposte.adminmobile.service;

import com.laposte.adminmobile.entities.TypeReclammation;
import com.laposte.adminmobile.repository.TypeReclammationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Boot port of {@code admin.mobile.dao.typereclamationdao}.
 */
@Service
public class TypeReclammationService {

    private final TypeReclammationRepository typeReclammationRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public TypeReclammationService(TypeReclammationRepository typeReclammationRepository) {
        this.typeReclammationRepository = typeReclammationRepository;
    }

    public List<TypeReclammation> allTypeRecla() {
        return typeReclammationRepository.findAll();
    }

    /**
     * Direct port of the legacy {@code userbyTelephone(ptelephone)}. Despite its name, the
     * original method called the "TypeReclammation.findByLibellerecla" NamedQuery while binding
     * the parameter under the name "telephone" - {@code TypeReclammation} has no telephone field
     * at all, so as written it would have thrown {@code IllegalArgumentException} at runtime
     * (dead/broken code). This preserves the query it actually referenced (by libellerecla) with
     * the parameter binding fixed, and keeps the misleading original name for traceability -
     * callers should treat it as a synonym of {@link #getTypeenreg(String)}.
     */
    public List<TypeReclammation> userbyTelephone(String ptelephone) {
        return typeReclammationRepository.findByLibellerecla(ptelephone);
    }

    public TypeReclammation find(BigDecimal id) {
        return typeReclammationRepository.findById(id).orElse(null);
    }

    @Transactional
    public TypeReclammation create(TypeReclammation a) {
        return typeReclammationRepository.save(a);
    }

    @Transactional
    public void delete(TypeReclammation a) {
        typeReclammationRepository.delete(a);
    }

    @Transactional
    public TypeReclammation edit(TypeReclammation a) {
        return typeReclammationRepository.save(a);
    }

    @Transactional
    public void refresh(TypeReclammation a) {
        entityManager.refresh(a);
    }

    public List<TypeReclammation> getTypeenreg(String libellerecla) {
        return typeReclammationRepository.findByLibellerecla(libellerecla);
    }
}
