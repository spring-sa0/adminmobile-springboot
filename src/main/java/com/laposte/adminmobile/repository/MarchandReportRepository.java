package com.laposte.adminmobile.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Backs {@code opmarchand.xhtml} / {@code listeopcom.xhtml} ("Liste des operations
 * marchands"/"...des marchands" - two near-identically titled legacy reports, both wired to the
 * same query here, see {@code MarchandController}) and {@code listecommicom.xhtml} ("Liste des
 * commissions des marchands"). Same Jasper-export provenance caveat as the sibling report
 * repositories in this module. A "merchant" account is confirmed to be an {@code EPOSTE.USERS_EPOSTE}
 * row with {@code IDPROFIL = 2} (see {@link com.laposte.adminmobile.repository.UsersRepository#findMerchantAccounts()});
 * these queries scope {@code SPT.RECU_ELECTRONIQUE} to that set of telephones, the same
 * telephone-based cross-schema join {@code RecuElectroniqueRepository#findEposteScopedOperationsBetween}
 * already uses for "Eposte operations" elsewhere in this codebase.
 */
@Repository
public class MarchandReportRepository {

    // RECU_ELECTRONIQUE.TELEPHONE porte l'indicatif (228 + 8 chiffres), USERS_EPOSTE.TELEPHONE non.
    private static final String MERCHANT_TEL_SUBQUERY =
            "(SELECT '228' || telephone FROM EPOSTE.USERS_EPOSTE WHERE idprofil = 2)";

    private static final String SQL_OPERATIONS =
            "SELECT r.telephone, r.numero_compte, r.montant, r.date_operation, "
            + "NVL(o.libelle_operation, 'Operation ' || r.code_operation) AS libelle_operation, r.numero_operation "
            + "FROM SPT.RECU_ELECTRONIQUE r LEFT JOIN SPT.OPERATION o ON o.code_operation = r.code_operation "
            + "WHERE r.date_operation BETWEEN ? AND ? AND r.telephone IN " + MERCHANT_TEL_SUBQUERY
            + " ORDER BY r.date_operation DESC";

    private static final String SQL_COMMISSIONS =
            "SELECT (r.telephone || ' / Cpte ' || r.numero_compte) AS label, COUNT(*) AS nb, "
            + "SUM(r.montant) AS total_montant, SUM(NVL(r.com_operation,0) + NVL(r.com_spt,0)) AS total_com "
            + "FROM SPT.RECU_ELECTRONIQUE r WHERE r.date_operation BETWEEN ? AND ? "
            + "AND r.telephone IN " + MERCHANT_TEL_SUBQUERY
            + " GROUP BY r.telephone, r.numero_compte ORDER BY total_com DESC";

    private final JdbcTemplate jdbcTemplate;

    public MarchandReportRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /** Shared by /marchand/operations and /marchand/operations-liste - see class javadoc. */
    public List<MerchantOperationRow> operations(Date date1, Date date2) {
        return jdbcTemplate.query(SQL_OPERATIONS, MarchandReportRepository::mapOperation, date1, date2);
    }

    public List<ReportAggregateRow> commissions(Date date1, Date date2) {
        return jdbcTemplate.query(SQL_COMMISSIONS, MarchandReportRepository::mapAggregate, date1, date2);
    }

    private static MerchantOperationRow mapOperation(ResultSet rs, int rowNum) throws SQLException {
        return new MerchantOperationRow(
                rs.getString("telephone"),
                rs.getBigDecimal("numero_compte"),
                rs.getBigDecimal("montant"),
                rs.getTimestamp("date_operation"),
                rs.getString("libelle_operation"),
                rs.getBigDecimal("numero_operation"));
    }

    private static ReportAggregateRow mapAggregate(ResultSet rs, int rowNum) throws SQLException {
        return new ReportAggregateRow(
                rs.getString("label"),
                (Number) rs.getObject("nb"),
                (Number) rs.getObject("total_montant"),
                (Number) rs.getObject("total_com"));
    }

    /** One merchant EcoCCP operation row. */
    public static final class MerchantOperationRow {
        private final String telephone;
        private final java.math.BigDecimal numeroCompte;
        private final java.math.BigDecimal montant;
        private final Date dateOperation;
        private final String libelleOperation;
        private final java.math.BigDecimal numeroOperation;

        public MerchantOperationRow(String telephone, java.math.BigDecimal numeroCompte,
                                     java.math.BigDecimal montant, Date dateOperation,
                                     String libelleOperation, java.math.BigDecimal numeroOperation) {
            this.telephone = telephone;
            this.numeroCompte = numeroCompte;
            this.montant = montant;
            this.dateOperation = dateOperation;
            this.libelleOperation = libelleOperation;
            this.numeroOperation = numeroOperation;
        }

        public String getTelephone() {
            return telephone;
        }

        public java.math.BigDecimal getNumeroCompte() {
            return numeroCompte;
        }

        public java.math.BigDecimal getMontant() {
            return montant;
        }

        public Date getDateOperation() {
            return dateOperation;
        }

        public String getLibelleOperation() {
            return libelleOperation;
        }

        public java.math.BigDecimal getNumeroOperation() {
            return numeroOperation;
        }
    }
}
