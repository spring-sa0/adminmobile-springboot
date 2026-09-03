package com.laposte.adminmobile.repository;

import com.laposte.adminmobile.entities.Support;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA port of {@code admin.mobile.dao.supportdao}.
 */
public interface SupportRepository extends JpaRepository<Support, BigDecimal> {

    /** Port of {@code supportdao.getSupportenreg(telephonsupport)}. */
    List<Support> findByTelephonsupport(String telephonsupport);
}
