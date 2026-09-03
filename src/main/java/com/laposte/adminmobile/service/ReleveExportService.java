package com.laposte.adminmobile.service;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

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
 * <p><b>Inherited risk (not fixed by this port):</b> these procedures TRUNCATE-and-refill shared,
 * ungated staging tables, exactly as the legacy JSF app did (a {@code @SessionScoped} bean calling
 * stateful stored procs against global tables with no per-user/session column). Two admins running
 * an export at the same time can and will read each other's rows, or a race can leave a report
 * empty. A real fix requires changing the PL/SQL procedures/tables (e.g. keying them by session id)
 * and is out of scope for this Java-side port.
 *
 * <p>Also note: the legacy methods (genafb120/proceduresolde/proceduresexcel) silently swallowed
 * {@code SQLException}. Here, {@link JdbcTemplate} lets it surface as an unchecked
 * {@link DataAccessException} instead - deliberately more visible than the original's silent
 * failure (which would otherwise produce a confusing empty/stale report with no error at all).
 */
@Service
public class ReleveExportService {

    private final JdbcTemplate jdbcTemplate;

    public ReleveExportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
