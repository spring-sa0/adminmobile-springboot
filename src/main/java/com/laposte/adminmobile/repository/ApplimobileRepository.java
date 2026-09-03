package com.laposte.adminmobile.repository;

import com.laposte.adminmobile.entities.Applimobile;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA port of {@code admin.mobile.dao.applidao}.
 */
public interface ApplimobileRepository extends JpaRepository<Applimobile, BigDecimal> {
}
