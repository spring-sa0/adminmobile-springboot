package com.laposte.adminmobile.repository;

import com.laposte.adminmobile.entities.Reclamations;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA port of {@code admin.mobile.dao.reclamationdao}.
 */
public interface ReclamationsRepository extends JpaRepository<Reclamations, BigDecimal> {

    /** Port of {@code reclamationdao.getReclamationNR()}: complaints whose état is 'NR' (non résolue). */
    @Query("SELECT r FROM Reclamations r WHERE r.idetat.idetat = 'NR'")
    List<Reclamations> findNonResolues();

    /**
     * Port of {@code reclamationdao.getReclaBySupport(codeutilisateur)}: complaints in état 'A'
     * (affectée) assigned to the given support agent.
     */
    @Query("SELECT r FROM Reclamations r WHERE r.idetat.idetat = 'A' AND r.idsupport.codeutilisateur = :codeutilisateur")
    List<Reclamations> findAffecteesByCodeutilisateur(@Param("codeutilisateur") String codeutilisateur);

    /**
     * Backs the {@code listerecla.xhtml} / {@code EtatbeanCtrl} report screen (ported here as an
     * on-screen filtered list, see {@code ReclamationController} - the legacy screen only offered
     * PDF/Excel exports of this same query, no Jasper/POI reporting is set up in this port).
     * {@code etat} mirrors the legacy select values: "REGLE" (idetat = R), "NON REGLE"
     * (idetat = NR), "TOUT" or null for no restriction. {@code debut}/{@code fin} are inclusive
     * bounds on datereclamation; either may be null for an open-ended bound.
     */
    @Query("SELECT r FROM Reclamations r WHERE "
            + "(:etat IS NULL OR :etat = 'TOUT' "
            + "  OR (:etat = 'REGLE' AND r.idetat.idetat = 'R') "
            + "  OR (:etat = 'NON REGLE' AND r.idetat.idetat = 'NR')) "
            + "AND (:debut IS NULL OR r.datereclamation >= :debut) "
            + "AND (:fin IS NULL OR r.datereclamation <= :fin) "
            + "ORDER BY r.datereclamation DESC")
    List<Reclamations> findForReport(@Param("etat") String etat, @Param("debut") Date debut, @Param("fin") Date fin);
}
