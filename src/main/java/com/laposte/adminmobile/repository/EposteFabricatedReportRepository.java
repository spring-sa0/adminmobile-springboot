package com.laposte.adminmobile.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * <b>TODO / FLAG - unverified schema, read before relying on this in production.</b>
 * <p>
 * Backs {@code listechequier.xhtml} ("ETAT LISTE DES DEMANDES DE CHEQUIERS") and
 * {@code etatlistecredit.xhtml} ("LISTE D'ACHAT DE CREDIT MOOV"). Unlike every other JdbcTemplate
 * repository in this module, these two tables ({@code EPOSTE.CHEQUIER}, {@code EPOSTE.ACHAT_CREDIT})
 * appear NOWHERE in the legacy source tree - not in {@code EtatbeanCtrl} (which only names the
 * compiled {@code .jasper} report files, {@code etatchequier.jasper}/{@code listeachatcredit.jasper},
 * neither of which is present as a {@code .jrxml} or {@code .jasper} binary in this checkout), not
 * in any DAO, and not among the 18 JPA entities already ported. There is therefore zero recoverable
 * evidence of these tables' real names or columns.
 * <p>
 * The table/column names below are an invented, plausible guess (a postal "cheque book" request
 * table keyed by the Eposte account holder; a "buy phone credit via Eposte balance" log), written
 * so the screens are genuinely wired to a real JDBC query rather than stubbed out - but they will
 * very likely need correcting against the real Oracle schema before this is production-usable.
 * Whoever has DB access should confirm the real table/column names and update the SQL here (the
 * query shape - date-range filter, ordered by date - should not need to change).
 */
@Repository
public class EposteFabricatedReportRepository {

    /** TODO: table/columns unverified - see class javadoc. */
    private static final String SQL_CHEQUIERS =
            "SELECT TELEPHONE, NUMERO_COMPTE, DATE_DEMANDE, STATUT FROM EPOSTE.CHEQUIER "
            + "WHERE DATE_DEMANDE BETWEEN ? AND ? ORDER BY DATE_DEMANDE DESC";

    /** TODO: table/columns unverified - see class javadoc. */
    private static final String SQL_ACHATS_CREDIT =
            "SELECT TELEPHONE, MONTANT, DATE_ACHAT, OPERATEUR FROM EPOSTE.ACHAT_CREDIT "
            + "WHERE DATE_ACHAT BETWEEN ? AND ? ORDER BY DATE_ACHAT DESC";

    private final JdbcTemplate jdbcTemplate;

    public EposteFabricatedReportRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<ChequierRow> chequiers(Date date1, Date date2) {
        return jdbcTemplate.query(SQL_CHEQUIERS, EposteFabricatedReportRepository::mapChequier, date1, date2);
    }

    public List<AchatCreditRow> achatsCredit(Date date1, Date date2) {
        return jdbcTemplate.query(SQL_ACHATS_CREDIT, EposteFabricatedReportRepository::mapAchatCredit, date1, date2);
    }

    private static ChequierRow mapChequier(ResultSet rs, int rowNum) throws SQLException {
        return new ChequierRow(rs.getString("TELEPHONE"), rs.getBigDecimal("NUMERO_COMPTE"),
                rs.getTimestamp("DATE_DEMANDE"), rs.getString("STATUT"));
    }

    private static AchatCreditRow mapAchatCredit(ResultSet rs, int rowNum) throws SQLException {
        return new AchatCreditRow(rs.getString("TELEPHONE"), rs.getBigDecimal("MONTANT"),
                rs.getTimestamp("DATE_ACHAT"), rs.getString("OPERATEUR"));
    }

    public static final class ChequierRow {
        private final String telephone;
        private final java.math.BigDecimal numeroCompte;
        private final Date dateDemande;
        private final String statut;

        public ChequierRow(String telephone, java.math.BigDecimal numeroCompte, Date dateDemande, String statut) {
            this.telephone = telephone;
            this.numeroCompte = numeroCompte;
            this.dateDemande = dateDemande;
            this.statut = statut;
        }

        public String getTelephone() {
            return telephone;
        }

        public java.math.BigDecimal getNumeroCompte() {
            return numeroCompte;
        }

        public Date getDateDemande() {
            return dateDemande;
        }

        public String getStatut() {
            return statut;
        }
    }

    public static final class AchatCreditRow {
        private final String telephone;
        private final java.math.BigDecimal montant;
        private final Date dateAchat;
        private final String operateur;

        public AchatCreditRow(String telephone, java.math.BigDecimal montant, Date dateAchat, String operateur) {
            this.telephone = telephone;
            this.montant = montant;
            this.dateAchat = dateAchat;
            this.operateur = operateur;
        }

        public String getTelephone() {
            return telephone;
        }

        public java.math.BigDecimal getMontant() {
            return montant;
        }

        public Date getDateAchat() {
            return dateAchat;
        }

        public String getOperateur() {
            return operateur;
        }
    }
}
