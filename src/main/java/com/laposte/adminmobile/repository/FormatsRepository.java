package com.laposte.adminmobile.repository;

import com.laposte.adminmobile.entities.Formats;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA port of {@code admin.mobile.dao.formatDao}.
 */
public interface FormatsRepository extends JpaRepository<Formats, BigDecimal> {
}
