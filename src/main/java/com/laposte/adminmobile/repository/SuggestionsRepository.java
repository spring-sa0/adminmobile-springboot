package com.laposte.adminmobile.repository;

import com.laposte.adminmobile.entities.Suggestions;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA port of {@code admin.mobile.dao.sugestiondao}.
 */
public interface SuggestionsRepository extends JpaRepository<Suggestions, BigDecimal> {

    /** Most recent suggestions first, for the /suggestions screen's recap list. */
    List<Suggestions> findAllByOrderByDatesugestDesc();

    /** Added for {@code /suggestions/liste} (legacy {@code listesugest.xhtml}, date-range filter). */
    List<Suggestions> findByDatesugestBetweenOrderByDatesugestDesc(Date date1, Date date2);
}
