package com.laposte.adminmobile.repository;

import com.laposte.adminmobile.entities.Frequences;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA port of {@code admin.mobile.dao.frequenceDao}.
 */
public interface FrequencesRepository extends JpaRepository<Frequences, BigDecimal> {
}
