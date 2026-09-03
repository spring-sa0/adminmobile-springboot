package com.laposte.adminmobile.repository;

import com.laposte.adminmobile.entities.TypeReclammation;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA port of {@code admin.mobile.dao.typereclamationdao}.
 */
public interface TypeReclammationRepository extends JpaRepository<TypeReclammation, BigDecimal> {

    /**
     * Backs both {@code typereclamationdao.getTypeenreg(libellerecla)} and (see
     * {@link com.laposte.adminmobile.service.TypeReclammationService#userbyTelephone}) the legacy
     * {@code userbyTelephone(ptelephone)} method.
     */
    List<TypeReclammation> findByLibellerecla(String libellerecla);
}
