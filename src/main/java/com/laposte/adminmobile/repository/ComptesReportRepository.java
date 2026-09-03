package com.laposte.adminmobile.repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Backs the "Etats &gt; Comptes" screens ({@code nbrecpteco.xhtml}, {@code listecompteco.xhtml},
 * {@code cptactif.xhtml}, {@code soldecompte.xhtml}, {@code listesoldecompte.xhtml},
 * {@code comptenegatif.xhtml}). Every one of these legacy screens rendered nothing on-screen -
 * they only triggered a JasperReports PDF/XLS export ({@code EtatbeanCtrl.etat*}), and the report's
 * SQL lived inside a compiled {@code .jasper} binary that is not present in the legacy source tree
 * (unlike {@code banbankxls.jrxml}, no {@code .jrxml} source survives for any of these). There is
 * also no JPA entity for {@code SPT.COMPTE} in this port (a cross-schema table outside the 18
 * modeled entities), so - per the module instructions - this talks to it directly via JdbcTemplate.
 * <p>
 * {@code SPT.COMPTE.NUMERO_COMPTE}/{@code INTITULE}/{@code NUMERO_CLIENT} are confirmed real
 * columns (used verbatim elsewhere in the legacy source: {@code usersdao.getIntitule},
 * {@code RecuElectroniquedao.recupNomprenomcom}). {@code DATE_CREATION} and {@code ACTIF} are NOT
 * confirmed anywhere in the legacy source for this table - they are a best-faith guess by analogy
 * with {@code EPOSTE.USERS_EPOSTE} (which does have both) - see the TODOs below and in
 * {@code ComptesController}/the Thymeleaf templates. The solde/negatifs queries avoid this problem
 * entirely by computing balances from {@code SPT.RECU_ELECTRONIQUE.SOLDE_APRES}, a confirmed
 * column, instead of guessing at a running-balance column on SPT.COMPTE.
 */
@Repository
public class ComptesReportRepository {

    /** TODO: DATE_CREATION on SPT.COMPTE is an unconfirmed guess - see class javadoc. */
    private static final String SQL_COUNT =
            "SELECT COUNT(*) FROM SPT.COMPTE WHERE DATE_CREATION BETWEEN ? AND ?";

    private static final String SQL_LIST =
            "SELECT NUMERO_COMPTE, INTITULE, NUMERO_CLIENT, DATE_CREATION FROM SPT.COMPTE "
            + "WHERE DATE_CREATION BETWEEN ? AND ? ORDER BY DATE_CREATION DESC";

    /** TODO: ACTIF on SPT.COMPTE, and the value 1 meaning "actif", are both unconfirmed guesses. */
    private static final String SQL_LIST_ACTIFS =
            "SELECT NUMERO_COMPTE, INTITULE, NUMERO_CLIENT, DATE_CREATION FROM SPT.COMPTE "
            + "WHERE DATE_CREATION BETWEEN ? AND ? AND ACTIF = 1 ORDER BY DATE_CREATION DESC";

    /** Period-end balance for accounts that had at least one EcoCCP movement in the range. */
    private static final String SQL_SOLDE =
            "SELECT r.numero_compte, c.intitule, r.solde_apres, r.date_operation "
            + "FROM SPT.RECU_ELECTRONIQUE r JOIN SPT.COMPTE c ON c.numero_compte = r.numero_compte "
            + "WHERE r.date_operation BETWEEN ? AND ? "
            + "AND r.date_operation = (SELECT MAX(r2.date_operation) FROM SPT.RECU_ELECTRONIQUE r2 "
            + "  WHERE r2.numero_compte = r.numero_compte AND r2.date_operation BETWEEN ? AND ?) "
            + "ORDER BY c.intitule";

    /** Every account, with its period-end balance if it moved in the range (blank otherwise). */
    private static final String SQL_LISTE_SOLDE =
            "SELECT c.numero_compte, c.intitule, "
            + "  (SELECT r.solde_apres FROM SPT.RECU_ELECTRONIQUE r WHERE r.numero_compte = c.numero_compte "
            + "     AND r.date_operation BETWEEN ? AND ? "
            + "     AND r.date_operation = (SELECT MAX(r2.date_operation) FROM SPT.RECU_ELECTRONIQUE r2 "
            + "       WHERE r2.numero_compte = c.numero_compte AND r2.date_operation BETWEEN ? AND ?)) AS solde, "
            + "  (SELECT MAX(r.date_operation) FROM SPT.RECU_ELECTRONIQUE r WHERE r.numero_compte = c.numero_compte "
            + "     AND r.date_operation BETWEEN ? AND ?) AS date_operation "
            + "FROM SPT.COMPTE c ORDER BY c.intitule";

    /** No date filter, matching comptenegatif.xhtml (a bare "GENERER" button, no date fields). */
    private static final String SQL_NEGATIFS =
            "SELECT r.numero_compte, c.intitule, r.solde_apres, r.date_operation "
            + "FROM SPT.RECU_ELECTRONIQUE r JOIN SPT.COMPTE c ON c.numero_compte = r.numero_compte "
            + "WHERE r.date_operation = (SELECT MAX(r2.date_operation) FROM SPT.RECU_ELECTRONIQUE r2 "
            + "  WHERE r2.numero_compte = r.numero_compte) "
            + "AND r.solde_apres < 0 ORDER BY r.solde_apres ASC";

    private final JdbcTemplate jdbcTemplate;

    public ComptesReportRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public Long countComptes(Date date1, Date date2) {
        return jdbcTemplate.queryForObject(SQL_COUNT, Long.class, date1, date2);
    }

    public List<CompteRow> listComptes(Date date1, Date date2) {
        return jdbcTemplate.query(SQL_LIST, ComptesReportRepository::mapCompteRow, date1, date2);
    }

    public List<CompteRow> listComptesActifs(Date date1, Date date2) {
        return jdbcTemplate.query(SQL_LIST_ACTIFS, ComptesReportRepository::mapCompteRow, date1, date2);
    }

    public List<CompteSoldeRow> solde(Date date1, Date date2) {
        return jdbcTemplate.query(SQL_SOLDE, ComptesReportRepository::mapSoldeRow, date1, date2, date1, date2);
    }

    public List<CompteSoldeRow> listeSolde(Date date1, Date date2) {
        return jdbcTemplate.query(SQL_LISTE_SOLDE, ComptesReportRepository::mapSoldeRow,
                date1, date2, date1, date2, date1, date2);
    }

    public List<CompteSoldeRow> comptesNegatifs() {
        return jdbcTemplate.query(SQL_NEGATIFS, ComptesReportRepository::mapSoldeRow);
    }

    private static CompteRow mapCompteRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new CompteRow(
                rs.getBigDecimal("NUMERO_COMPTE"),
                rs.getString("INTITULE"),
                rs.getBigDecimal("NUMERO_CLIENT"),
                rs.getTimestamp("DATE_CREATION"));
    }

    private static CompteSoldeRow mapSoldeRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new CompteSoldeRow(
                rs.getBigDecimal("numero_compte"),
                rs.getString("intitule"),
                rs.getBigDecimal("solde_apres"),
                rs.getTimestamp("date_operation"));
    }

    /** One SPT.COMPTE row. */
    public static final class CompteRow {
        private final BigDecimal numeroCompte;
        private final String intitule;
        private final BigDecimal numeroClient;
        private final Date dateCreation;

        public CompteRow(BigDecimal numeroCompte, String intitule, BigDecimal numeroClient, Date dateCreation) {
            this.numeroCompte = numeroCompte;
            this.intitule = intitule;
            this.numeroClient = numeroClient;
            this.dateCreation = dateCreation;
        }

        public BigDecimal getNumeroCompte() {
            return numeroCompte;
        }

        public String getIntitule() {
            return intitule;
        }

        public BigDecimal getNumeroClient() {
            return numeroClient;
        }

        public Date getDateCreation() {
            return dateCreation;
        }
    }

    /** One account's computed period-end balance. */
    public static final class CompteSoldeRow {
        private final BigDecimal numeroCompte;
        private final String intitule;
        private final BigDecimal solde;
        private final Date dateOperation;

        public CompteSoldeRow(BigDecimal numeroCompte, String intitule, BigDecimal solde, Date dateOperation) {
            this.numeroCompte = numeroCompte;
            this.intitule = intitule;
            this.solde = solde;
            this.dateOperation = dateOperation;
        }

        public BigDecimal getNumeroCompte() {
            return numeroCompte;
        }

        public String getIntitule() {
            return intitule;
        }

        public BigDecimal getSolde() {
            return solde;
        }

        public Date getDateOperation() {
            return dateOperation;
        }
    }
}
