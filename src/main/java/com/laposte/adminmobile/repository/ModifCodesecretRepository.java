package com.laposte.adminmobile.repository;

import com.laposte.adminmobile.entities.ModifCodesecret;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA port of {@code admin.mobile.dao.modifsecretdao2}.
 */
public interface ModifCodesecretRepository extends JpaRepository<ModifCodesecret, BigDecimal> {

    /**
     * Added for {@code /eposte/modif-pwd} (legacy {@code etatmodifpwd.xhtml}, "LISTE DES PASSWORD
     * MODIFIES"): filters on {@code DATEETHEURE}, the timestamp of the password/code-secret change
     * itself (as opposed to {@code DATE_CREATION}, which - by analogy with {@link
     * com.laposte.adminmobile.entities.Users} - is more likely the row's own creation date).
     */
    List<ModifCodesecret> findByDateetheureBetweenOrderByDateetheureDesc(Date date1, Date date2);
}
