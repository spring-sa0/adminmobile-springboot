package com.laposte.adminmobile.repository;

import com.laposte.adminmobile.entities.Users;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA port of {@code admin.mobile.dao.usersdao}.
 * <p>
 * Several legacy methods ran hand-written {@code UPDATE EPOSTE.USERS_EPOSTE ...} statements that
 * touch columns not mapped on the {@link Users} entity (in particular {@code CODE_PASSE}, which
 * only exists on the sibling {@code UsersEposte} entity even though both map to the same
 * {@code USERS_EPOSTE} table). Those are kept as native SQL {@code @Modifying} queries below
 * rather than JPQL, so they can reach every column the original DAO touched. The
 * {@code EPOSTE.} schema prefix from the legacy SQL is dropped: {@code Users} itself declares
 * {@code @Table(name = "USERS_EPOSTE")} with no schema, i.e. it already relies on the connection's
 * default schema being EPOSTE, so the native queries do the same for consistency.
 */
public interface UsersRepository extends JpaRepository<Users, String> {

    List<Users> findByTelephone(String telephone);

    /** Port of {@code usersdao.reinitPass(tel, pass)}. */
    @Modifying
    @Query(value = "UPDATE USERS_EPOSTE SET CODE_SECRET = :pass, ERR_CODE = 0, CODE_PASSE = 3000 WHERE TELEPHONE = :tel",
            nativeQuery = true)
    int reinitPass(@Param("tel") String tel, @Param("pass") String pass);

    /** Port of {@code usersdao.paramMarchand(tel, numcpte)}. */
    @Modifying
    @Query(value = "UPDATE USERS_EPOSTE SET NUMERO_COMPTE = :numcpte, IDPROFIL = 2 WHERE TELEPHONE = :tel",
            nativeQuery = true)
    int paramMarchand(@Param("tel") String tel, @Param("numcpte") String numcpte);

    /** Port of {@code usersdao.activerMarchand(tel)}. */
    @Modifying
    @Query(value = "UPDATE USERS_EPOSTE SET IDPROFIL = 2 WHERE TELEPHONE = :tel", nativeQuery = true)
    int activerMarchand(@Param("tel") String tel);

    /** Port of {@code usersdao.desactiverMarchand(tel)}. */
    @Modifying
    @Query(value = "UPDATE USERS_EPOSTE SET IDPROFIL = 1 WHERE TELEPHONE = :tel", nativeQuery = true)
    int desactiverMarchand(@Param("tel") String tel);

    /** Port of {@code usersdao.desactivecompte(tel)}. */
    @Modifying
    @Query(value = "UPDATE USERS_EPOSTE SET ACTIF = 2 WHERE TELEPHONE = :tel", nativeQuery = true)
    int desactivecompte(@Param("tel") String tel);

    /** Port of {@code usersdao.activecompte(tel)}. */
    @Modifying
    @Query(value = "UPDATE USERS_EPOSTE SET ACTIF = 0 WHERE TELEPHONE = :tel", nativeQuery = true)
    int activecompte(@Param("tel") String tel);

    /** Port of {@code usersdao.modifProfil(tel, newprofil, numcompte)}. */
    @Modifying
    @Query(value = "UPDATE USERS_EPOSTE SET IDPROFIL = :newprofil, NUMERO_COMPTE = :numcompte WHERE TELEPHONE = :tel",
            nativeQuery = true)
    int modifProfil(@Param("tel") String tel, @Param("newprofil") int newprofil, @Param("numcompte") String numcompte);

    /** Port of {@code usersdao.updateEmail(tel, email)}. */
    @Modifying
    @Query(value = "UPDATE USERS_EPOSTE SET MAIL = :email WHERE TELEPHONE = :tel", nativeQuery = true)
    int updateEmail(@Param("tel") String tel, @Param("email") String email);

    /**
     * Port of {@code usersdao.getProfilUtil(login)}: a lookup against {@code MBUDGET.UTILISATEUR_LOG},
     * belonging to an entirely different application (the budget app) and unrelated to
     * {@link Users} / EPOSTE. Kept as a native passthrough query for fidelity with the legacy
     * DAO's public surface.
     */
    @Query(value = "SELECT CODE_PROFIL FROM MBUDGET.UTILISATEUR_LOG WHERE LOWER(LOGIN) = :login", nativeQuery = true)
    Integer getProfilUtil(@Param("login") String login);

    /**
     * Port of {@code usersdao.getIntitule(numero)}: identical in the legacy code to
     * {@code formatDao.getIntitule} - a lookup against {@code SPT.COMPTE}, unrelated to
     * {@link Users}. See {@link com.laposte.adminmobile.service.FormatsService#getIntitule}.
     */
    @Query(value = "SELECT INTITULE FROM SPT.COMPTE WHERE NUMERO_COMPTE = :numero", nativeQuery = true)
    String getIntitule(@Param("numero") String numero);

    /**
     * Added for {@code /eposte/comptes-op-effectuees} (legacy {@code eposteopfait.xhtml}): Eposte
     * accounts that made at least one EcoCCP receipt in the date range, matched by telephone (the
     * key shared between {@code USERS_EPOSTE} and {@code SPT.RECU_ELECTRONIQUE}). See
     * {@code RecuElectroniqueRepository#findEposteScopedOperationsBetween} for why telephone is
     * used as the cross-table join key.
     */
    // RECU_ELECTRONIQUE.TELEPHONE porte l'indicatif (228 + 8 chiffres), USERS_EPOSTE.TELEPHONE non :
    // sans CONCAT, aucune ligne ne correspondait jamais.
    @Query("SELECT u FROM Users u WHERE CONCAT('228', u.telephone) IN "
            + "(SELECT DISTINCT r.telephone FROM RecuElectronique r WHERE r.dateOperation BETWEEN :date1 AND :date2)")
    List<Users> findUsersWithOperationsBetween(@Param("date1") java.util.Date date1, @Param("date2") java.util.Date date2);

    /**
     * Complement of the above, for {@code /eposte/comptes-op-non-effectuees} (legacy {@code eposteoppasfait.xhtml}).
     * The {@code IS NOT NULL} guard is required: a single receipt with a NULL telephone makes
     * {@code NOT IN} evaluate to UNKNOWN for every row, so the list would always come back empty.
     */
    @Query("SELECT u FROM Users u WHERE CONCAT('228', u.telephone) NOT IN "
            + "(SELECT DISTINCT r.telephone FROM RecuElectronique r WHERE r.dateOperation BETWEEN :date1 AND :date2 "
            + "AND r.telephone IS NOT NULL)")
    List<Users> findUsersWithoutOperationsBetween(@Param("date1") java.util.Date date1, @Param("date2") java.util.Date date2);

    /**
     * Added for the Comptes/Marchand/Recap/Commissions module (see {@code ComptesController} etc).
     * Port of {@code etatcomptedesa.xhtml} ("Compte desactives"): {@code usersdao.desactivecompte(tel)}
     * sets {@code ACTIF = 2}, {@code activecompte(tel)} resets it to {@code 0} - so {@code ACTIF = 2}
     * is a confirmed, non-approximated marker of a deactivated Eposte account.
     */
    List<Users> findByActifOrderByDateCreationDesc(BigInteger actif);

    /**
     * Port of {@code souscript.xhtml} ("Etat de Souscription EPOSTE"): interpreted as new Eposte
     * subscriptions in a period (row created), optionally narrowed by the {@code origine} column -
     * which the legacy screen's operateur dropdown values (TMoney/FLooz) plausibly match, since
     * {@code origine} is the same column {@code EtatbeanCtrl}'s TMoney/FLooz dispatch logic keys off
     * of elsewhere. Used when the "Tout" / blank operateur choice is NOT selected.
     */
    List<Users> findByDateCreationBetweenAndOrigineOrderByDateCreationDesc(Date date1, Date date2, String origine);

    /** Same as above with no operateur filter - used for the "Tout" / blank choice. */
    List<Users> findByDateCreationBetweenOrderByDateCreationDesc(Date date1, Date date2);

    /**
     * Merchant ("Marchand") accounts: {@code usersdao.activerMarchand}/{@code paramMarchand} both
     * set {@code IDPROFIL = 2} on the Eposte account to mark it a merchant profile, and
     * {@code desactiverMarchand} resets it to {@code 1} - so {@code IDPROFIL = 2} is a confirmed
     * marker, not a guess. Backs every {@code /marchand/**} screen in {@code MarchandController}.
     */
    @Query("SELECT u FROM Users u WHERE u.idprofil.idprofil = 2 ORDER BY u.dateCreation DESC")
    List<Users> findMerchantAccounts();
}
