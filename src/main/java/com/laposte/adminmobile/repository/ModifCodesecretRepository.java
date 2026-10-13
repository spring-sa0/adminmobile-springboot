package com.laposte.adminmobile.repository;

import com.laposte.adminmobile.entities.ModifCodesecret;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA port of {@code admin.mobile.dao.modifsecretdao2}.
 * <p>
 * Volontairement un simple {@link Repository} et non un JpaRepository : TELEPHONE n'est pas
 * unique dans ce journal, donc findById/save/delete sont faux par construction (voir
 * {@link ModifCodesecret}). Les lectures passent par une expression constructeur : les objets
 * renvoyes ne sont pas geres, chaque ligne reste distincte meme pour un meme telephone.
 */
public interface ModifCodesecretRepository extends Repository<ModifCodesecret, BigDecimal> {

    /**
     * Added for {@code /eposte/modif-pwd} (legacy {@code etatmodifpwd.xhtml}, "LISTE DES PASSWORD
     * MODIFIES"): filters on {@code DATEETHEURE}, the timestamp of the password/code-secret change
     * itself (as opposed to {@code DATE_CREATION}, which - by analogy with {@link
     * com.laposte.adminmobile.entities.Users} - is more likely the row's own creation date).
     */
    @Query("SELECT new com.laposte.adminmobile.entities.ModifCodesecret("
            + "m.telephone, m.login, m.pwd, m.dateCreation, m.actif, m.codeSecret, m.idprofil, "
            + "m.origine, m.nom, m.prenom, m.dateetheure, m.utilisateur, m.motif) "
            + "FROM ModifCodesecret m "
            + "WHERE m.dateetheure BETWEEN :date1 AND :date2 "
            + "ORDER BY m.dateetheure DESC")
    List<ModifCodesecret> findByDateetheureBetweenOrderByDateetheureDesc(@Param("date1") Date date1,
                                                                         @Param("date2") Date date2);
}
