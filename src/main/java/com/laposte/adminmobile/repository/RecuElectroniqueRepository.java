package com.laposte.adminmobile.repository;

import com.laposte.adminmobile.entities.RecuElectronique;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA port of {@code admin.mobile.dao.RecuElectroniquedao}.
 * <p>
 * The legacy DAO's {@code recupListopBy*}/{@code recupop} family ran raw SQL joining
 * {@code spt.recu_electronique} to {@code spt.operation} on {@code code_operation}, then - for
 * every row found - re-queried {@code RecuElectronique} by {@code numero_operation} to build the
 * final list. Since every column that join filtered or selected on ({@code telephone},
 * {@code numero_compte}, {@code montant}, {@code date_operation}, {@code libelle_operation},
 * {@code code_operation}) already lives directly on {@link RecuElectronique}, the join added no
 * filtering power of its own; the methods below reproduce the same net result as plain JPQL
 * against the entity, without the redundant round trip.
 */
public interface RecuElectroniqueRepository extends JpaRepository<RecuElectronique, Long> {

    List<RecuElectronique> findByTelephoneOrderByDateOperationDesc(String telephone);

    List<RecuElectronique> findByNumeroCompteOrderByDateOperationDesc(Long numeroCompte);

    /** Port of {@code opbyPeriode(date1, date2)}. */
    List<RecuElectronique> findByDateOperationBetween(Date date1, Date date2);

    /** Port of {@code listopBynumeroOperation}/{@code recupListopBynumop}/{@code recupop}. */
    List<RecuElectronique> findByNumeroOperation(Long numeroOperation);

    List<RecuElectronique> findByNumeroOperationOrderByDateOperationDesc(Long numeroOperation);

    // ---- Controles autour des procedures d'annulation / restauration --------------------------
    // Requetes scalaires : un meme numero peut exister en plusieurs lignes (recu duplique a la source).

    /** Contre-ecriture creee par l'annulation de l'operation (null si l'operation n'est pas annulee). */
    @Query("SELECT MAX(r.numeroOperation) FROM RecuElectronique r WHERE r.ancienNumOp = :numeroOperation")
    Long contreEcritureDe(@Param("numeroOperation") Long numeroOperation);

    /** Si le numero est une contre-ecriture : l'operation qu'elle annule (sinon null). */
    @Query("SELECT MAX(r.ancienNumOp) FROM RecuElectronique r WHERE r.numeroOperation = :numeroOperation")
    Long operationAnnuleePar(@Param("numeroOperation") Long numeroOperation);

    @Query("SELECT COUNT(r) FROM RecuElectronique r WHERE r.numeroOperation = :numeroOperation")
    long nombreLignes(@Param("numeroOperation") Long numeroOperation);

    /** Port of {@code recupListopByTelephone(ptelephone, date1, date2)}. */
    List<RecuElectronique> findByTelephoneAndDateOperationBetweenOrderByDateOperationDesc(
            String telephone, Date date1, Date date2);

    /** Port of {@code recupListopByCompe(numerocompte, date1, date2)}. */
    List<RecuElectronique> findByNumeroCompteAndDateOperationBetweenOrderByDateOperationDesc(
            Long numeroCompte, Date date1, Date date2);

    /** Port of {@code recupListopByRefid(refbank)}. */
    List<RecuElectronique> findByRefbank(String refbank);

    /** Port of {@code recupListop(date1, date2, codeop)}. */
    List<RecuElectronique> findByDateOperationBetweenAndCodeOperationOrderByDateOperationDesc(
            Date date1, Date date2, BigInteger codeOperation);

    /**
     * Added for the "operations / transactions / consultations" module ports (screens with no
     * legacy on-screen {@code p:dataTable} to copy from - see per-screen TODOs in
     * {@code TransactionEtatController}/{@code EposteOperationController}): every EcoCCP
     * transaction receipt in a date range, most-recent first. Used as-is for
     * {@code /etats/transactions/ecoccp} and the un-filtered case of {@code /etats/transactions/global}.
     */
    List<RecuElectronique> findByDateOperationBetweenOrderByDateOperationDesc(Date date1, Date date2);

    /**
     * Same as above, additionally filtered by {@code origine} (the channel/operator column,
     * used here as a best-faith stand-in for the legacy Jasper reports' "Operateur" parameter -
     * see controller-level TODOs). Used for {@code bank-to-wallet}, {@code wallet-to-bank},
     * {@code retrait}, {@code depot}, the operator-filtered {@code global} report and
     * {@code gateway/liste}.
     */
    List<RecuElectronique> findByDateOperationBetweenAndOrigineOrderByDateOperationDesc(
            Date date1, Date date2, String origine);

    /**
     * Cancelled EcoCCP operations in a date range. PROCEDURES.ANNUL_OP_ECOCCP moves the original
     * row to SPT.RECU_ELECTRONIQUE_ANNULE and inserts here an "ANNUL ..." counter-entry whose
     * {@code ancienNumOp} points to the cancelled operation (only that procedure fills the column),
     * dated at cancellation time. Used for {@code /etats/transactions/ecoccp-annulees}.
     */
    List<RecuElectronique> findByAncienNumOpIsNotNullAndDateOperationBetweenOrderByDateOperationDesc(
            Date date1, Date date2);

    /**
     * Best-faith interpretation of "operations Eposte" (as opposed to plain EcoCCP operations):
     * restricts the EcoCCP receipt table to telephones that also have a USERS_EPOSTE account
     * (see {@link com.laposte.adminmobile.entities.Users}). There is no separate Eposte
     * transaction table reachable from the legacy source (the real legacy report was
     * Jasper-compiled; its SQL is not recoverable from the source tree - see module report
     * TODOs), so this is an approximation: it narrows the same EcoCCP receipts to phone numbers
     * known as Eposte subscribers. Used for {@code /eposte/operations} and
     * {@code /etats/transactions/compte-a-compte}.
     */
    @Query("SELECT r FROM RecuElectronique r WHERE r.dateOperation BETWEEN :date1 AND :date2 "
            // r.telephone porte l'indicatif 228, u.telephone non (voir UsersRepository).
            + "AND r.telephone IN (SELECT CONCAT('228', u.telephone) FROM Users u) ORDER BY r.dateOperation DESC")
    List<RecuElectronique> findEposteScopedOperationsBetween(@Param("date1") Date date1, @Param("date2") Date date2);

    // ---- Variantes paginees des recherches sur toute une periode -------------------------------
    // Ces etats portent sur toutes les operations de la periode (jusqu'a ~100 000 lignes sur
    // 92 jours) : la base ne renvoie que la page demandee, plus un comptage pour la pagination.
    // Memes filtres et meme tri que les variantes List ci-dessus.

    Page<RecuElectronique> findByDateOperationBetweenOrderByDateOperationDesc(Date date1, Date date2, Pageable page);

    Page<RecuElectronique> findByDateOperationBetweenAndOrigineOrderByDateOperationDesc(
            Date date1, Date date2, String origine, Pageable page);

    Page<RecuElectronique> findByAncienNumOpIsNotNullAndDateOperationBetweenOrderByDateOperationDesc(
            Date date1, Date date2, Pageable page);

    Page<RecuElectronique> findByDateOperationBetweenAndCodeOperationOrderByDateOperationDesc(
            Date date1, Date date2, BigInteger codeOperation, Pageable page);

    @Query(value = "SELECT r FROM RecuElectronique r WHERE r.dateOperation BETWEEN :date1 AND :date2 "
            + "AND r.telephone IN (SELECT CONCAT('228', u.telephone) FROM Users u) ORDER BY r.dateOperation DESC",
            countQuery = "SELECT COUNT(r) FROM RecuElectronique r WHERE r.dateOperation BETWEEN :date1 AND :date2 "
                    + "AND r.telephone IN (SELECT CONCAT('228', u.telephone) FROM Users u)")
    Page<RecuElectronique> findEposteScopedOperationsBetween(@Param("date1") Date date1, @Param("date2") Date date2,
                                                             Pageable page);

    /**
     * Port of {@code recupNomprenomcom(numcompte)}: a lookup against {@code spt.compte} /
     * {@code spt.client}, neither of which is one of the 18 JPA entities modeled in this port.
     * Kept as a native passthrough query for fidelity with the legacy DAO's public surface.
     */
    @Query(value = "SELECT cl.nom_client || ' ' || cl.prenom_client "
            + "FROM spt.compte c, spt.client cl "
            + "WHERE c.numero_client = cl.numero_client AND c.numero_compte = :numeroCompte",
            nativeQuery = true)
    String recupNomprenomcom(@Param("numeroCompte") String numeroCompte);
}
