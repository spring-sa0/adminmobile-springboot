package com.laposte.adminmobile.repository;

import com.laposte.adminmobile.entities.Profil;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA port of {@code admin.mobile.dao.profildao}.
 */
public interface ProfilRepository extends JpaRepository<Profil, BigDecimal> {
}
