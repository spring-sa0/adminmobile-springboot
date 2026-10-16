package com.laposte.adminmobile.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Port of the staging-table stored-procedure calls in admin.mobile.ctrl.EtatbeanCtrl that back
 * the two on-demand "releve" (account statement) exports:
 * <ul>
 *   <li>{@code IMOBILE.procedure_solde_tgcel2(numerocompte, datedebut, datefin)} populates
 *       {@code IMOBILE.MOUVEMENT_UL}, read by reports/releve_client1.jrxml (the "Excel AFB120"
 *       screen's spreadsheet export).</li>
 *   <li>{@code IMOBILE.procedure_solde_tgcel(numerocompte, datedebut, datefin)} populates
 *       {@code IMOBILE.MOUVEMENT}, read by reports/releve_solde.jrxml (the "Excel TOGOCEL"
 *       screen's spreadsheet export - see the class javadoc on ReleveExportController for why
 *       this is NOT op_togocel.jrxml despite the screen's name).</li>
 *   <li>{@code IMOBILE.gen_afb(numerocompte, datedebut, datefin)} populates
 *       {@code IMOBILE.MODELEAFB} with pre-formatted, fixed-width AFB120 lines, read back
 *       verbatim as plain text - no Jasper report is involved for this one.</li>
 * </ul>
 *
 * <p><b>Tables de travail partagees :</b> ces procedures vident puis remplissent des tables
 * globales, sans colonne d'utilisateur ou de session (heritage du legacy). Deux exports simultanes
 * liraient les lignes l'un de l'autre. Chaque export doit donc executer "remplir + relire" dans
 * {@link #enExclusivite} : un seul a la fois dans cette application. Limite : cela ne protege pas
 * d'un autre programme appelant les memes procedures (ancienne application JSF encore en service,
 * batch...) ; la vraie correction est cote PL/SQL (tables indexees par session).
 *
 * <p>Also note: the legacy methods (genafb120/proceduresolde/proceduresexcel) silently swallowed
 * {@code SQLException}. Here, {@link JdbcTemplate} lets it surface as an unchecked
 * {@link DataAccessException} instead - deliberately more visible than the original's silent
 * failure (which would otherwise produce a confusing empty/stale report with no error at all).
 */
@Service
public class ReleveExportService {

    /** Attente maximale d'un export quand un autre est en cours. */
    private static final long ATTENTE_SECONDES = 60;

    private final JdbcTemplate jdbcTemplate;
    private final long attenteSecondes;
    /** Equitable : les exports en attente passent dans l'ordre d'arrivee. */
    private final ReentrantLock verrouTablesDeTravail = new ReentrantLock(true);

    @Autowired
    public ReleveExportService(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, ATTENTE_SECONDES);
    }

    ReleveExportService(JdbcTemplate jdbcTemplate, long attenteSecondes) {
        this.jdbcTemplate = jdbcTemplate;
        this.attenteSecondes = attenteSecondes;
    }

    /**
     * Execute un export complet (remplissage de la table de travail puis lecture) sans qu'un autre
     * export ne puisse s'intercaler. Attend au plus {@value #ATTENTE_SECONDES} s qu'un export en
     * cours se termine, sinon leve {@link ExportOccupeException}.
     */
    public <T> T enExclusivite(Supplier<T> export) {
        boolean obtenu;
        try {
            obtenu = verrouTablesDeTravail.tryLock(attenteSecondes, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExportOccupeException();
        }
        if (!obtenu) {
            throw new ExportOccupeException();
        }
        try {
            return export.get();
        } finally {
            verrouTablesDeTravail.unlock();
        }
    }

    /** Un autre export de releve occupe les tables de travail depuis trop longtemps. */
    public static class ExportOccupeException extends RuntimeException {
        public ExportOccupeException() {
            super("Un autre export de releve est en cours. Reessayez dans quelques instants.");
        }
    }

    public void populateReleveClient1(String numeroCompte, LocalDate dateDebut, LocalDate dateFin) {
        callProcedure("{ call IMOBILE.procedure_solde_tgcel2(?, ?, ?) }", numeroCompte, dateDebut, dateFin);
    }

    public void populateReleveSolde(String numeroCompte, LocalDate dateDebut, LocalDate dateFin) {
        callProcedure("{ call IMOBILE.procedure_solde_tgcel(?, ?, ?) }", numeroCompte, dateDebut, dateFin);
    }

    public void populateAfb120(String numeroCompte, LocalDate dateDebut, LocalDate dateFin) {
        callProcedure("{ call IMOBILE.gen_afb(?, ?, ?) }", numeroCompte, dateDebut, dateFin);
    }

    /** Port of EtatbeanCtrl.FichierAFB()'s "select modele from imobile.modeleafb order by idmodele asc". */
    public List<String> fetchAfb120Lines() {
        return jdbcTemplate.queryForList("select modele from imobile.modeleafb order by idmodele asc", String.class);
    }

    private void callProcedure(String call, String numeroCompte, LocalDate dateDebut, LocalDate dateFin) {
        jdbcTemplate.execute((Connection connection) -> {
            try (CallableStatement stmt = connection.prepareCall(call)) {
                stmt.setString(1, numeroCompte);
                stmt.setDate(2, dateDebut != null ? Date.valueOf(dateDebut) : null);
                stmt.setDate(3, dateFin != null ? Date.valueOf(dateFin) : null);
                stmt.execute();
                return null;
            }
        });
    }
}
