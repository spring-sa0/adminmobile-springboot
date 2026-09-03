package com.laposte.adminmobile.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Backs {@code commissions.xhtml} ("Commissions EcoCCP") and {@code comeposte.xhtml} ("Commissions
 * EPOSTE"), both {@code EtatbeanCtrl} Jasper exports with no recoverable SQL (same caveat as
 * {@link RecapReportRepository}). Interpreted as an aggregate of the commission columns on
 * {@code SPT.RECU_ELECTRONIQUE} ({@code COM_OPERATION}/{@code COM_SPT}, both confirmed real
 * columns), grouped by {@code origine}, optionally narrowed to one operateur.
 * <p>
 * TODO / risk: {@code commissions.xhtml}'s operateur dropdown offers TOGOCEL/MOOV while
 * {@code comeposte.xhtml}'s offers TMoney/FLooz - two different label sets for what is presumably
 * the same two telecom operators (Togocom/T-Money vs Moov/Flooz). Both are passed straight through
 * as an equality filter on {@code origine} here; if the column's actual stored values don't match
 * either label set, the equality filter will (safely) just return zero rows rather than the wrong
 * ones - verify the real {@code origine} values in production before relying on the operateur
 * filter specifically. The unfiltered totals (date range only) are unaffected by this risk.
 */
@Repository
public class CommissionsReportRepository {

    private static final String SQL_BASE =
            "SELECT NVL(origine, '(tous)') AS label, COUNT(*) AS nb, SUM(montant) AS total_montant, "
            + "SUM(NVL(com_operation,0) + NVL(com_spt,0)) AS total_com "
            + "FROM SPT.RECU_ELECTRONIQUE WHERE date_operation BETWEEN ? AND ? ";

    private static final String SQL_GROUP = "GROUP BY origine ORDER BY total_com DESC";

    private final JdbcTemplate jdbcTemplate;

    public CommissionsReportRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * @param operateur when non-blank, narrows to {@code origine = operateur}; when blank/null,
     *                  every origine is grouped and shown.
     */
    public List<ReportAggregateRow> commissionsSummary(Date date1, Date date2, String operateur) {
        if (operateur == null || operateur.isBlank()) {
            String sql = SQL_BASE + SQL_GROUP;
            return jdbcTemplate.query(sql, CommissionsReportRepository::mapAggregate, date1, date2);
        }
        String sql = SQL_BASE + "AND origine = ? " + SQL_GROUP;
        return jdbcTemplate.query(sql, CommissionsReportRepository::mapAggregate, date1, date2, operateur);
    }

    private static ReportAggregateRow mapAggregate(ResultSet rs, int rowNum) throws SQLException {
        return new ReportAggregateRow(
                rs.getString("label"),
                (Number) rs.getObject("nb"),
                (Number) rs.getObject("total_montant"),
                (Number) rs.getObject("total_com"));
    }
}
