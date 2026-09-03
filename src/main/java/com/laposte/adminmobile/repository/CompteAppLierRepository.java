package com.laposte.adminmobile.repository;

import com.laposte.adminmobile.entities.CompteAppLier;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA port of {@code admin.mobile.dao.compteAppLierdao} (read-only in the legacy app -
 * no create/edit/delete/refresh existed there).
 */
public interface CompteAppLierRepository extends JpaRepository<CompteAppLier, BigDecimal> {

    List<CompteAppLier> findByTelephoneClient(String telephoneClient);
}
