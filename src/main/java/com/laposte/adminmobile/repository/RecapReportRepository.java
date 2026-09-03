package com.laposte.adminmobile.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Backs the "Eposte &gt; Recap operations / Recap global" screens ({@code recapclient.xhtml},
 * {@code recapcompte.xhtml}, {@code recapglobalprdt.xhtml}, {@code recapglobal.xhtml}). Same
 * provenance caveat as {@link ComptesReportRepository}: these legacy screens only triggered a
 * Jasper export with no recoverable SQL. "Recap" (recapitulatif = summary) is interpreted here as
 * an aggregate (count/sum) grouped by client, account or product, built on
 * {@code SPT.RECU_ELECTRONIQUE} (a confirmed, real, already-JPA-mapped table) - joined to
 * {@code SPT.COMPTE}/{@code SPT.OPERATION} for readable labels where those joins are cheap. Kept as
 * JdbcTemplate rather than JPQL because every query here either needs a GROUP BY the Spring Data
 * derived-query vocabulary can't express, or joins into {@code SPT.COMPTE} which has no JPA entity
 * (same reasoning {@link BankToBankReportRepository} used for its own cross-schema joins).
 * {@code recapglobalSouscriptions} is the one exception: "souscriptions" (subscriptions) maps to
 * new {@code EPOSTE.USERS_EPOSTE} rows rather than EcoCCP operations, grouped by {@code origine}.
 */
@Repository
public class RecapReportRepository {

    private static final String SQL_PAR_CLIENT =
            "SELECT telephone AS label, COUNT(*) AS nb, SUM(montant) AS total_montant, "
            + "SUM(NVL(com_operation,0) + NVL(com_spt,0)) AS total_com "
            + "FROM SPT.RECU_ELECTRONIQUE WHERE date_operation BETWEEN ? AND ? "
            + "GROUP BY telephone ORDER BY total_montant DESC";

    private static final String SQL_PAR_COMPTE =
            "SELECT NVL(c.intitule, 'Compte ' || r.numero_compte) AS label, COUNT(*) AS nb, "
            + "SUM(r.montant) AS total_montant, SUM(NVL(r.com_operation,0) + NVL(r.com_spt,0)) AS total_com "
            + "FROM SPT.RECU_ELECTRONIQUE r LEFT JOIN SPT.COMPTE c ON c.numero_compte = r.numero_compte "
            + "WHERE r.date_operation BETWEEN ? AND ? "
            + "GROUP BY r.numero_compte, c.intitule ORDER BY total_montant DESC";

    private static final String SQL_PAR_PRODUIT =
            "SELECT NVL(o.libelle_operation, 'Operation ' || r.code_operation) AS label, COUNT(*) AS nb, "
            + "SUM(r.montant) AS total_montant, SUM(NVL(r.com_operation,0) + NVL(r.com_spt,0)) AS total_com "
            + "FROM SPT.RECU_ELECTRONIQUE r LEFT JOIN SPT.OPERATION o ON o.code_operation = r.code_operation "
            + "WHERE r.date_operation BETWEEN ? AND ? "
            + "GROUP BY o.libelle_operation, r.code_operation ORDER BY total_montant DESC";

    private static final String SQL_SOUSCRIPTIONS =
            "SELECT NVL(origine, '(non renseigne)') AS label, COUNT(*) AS nb "
            + "FROM EPOSTE.USERS_EPOSTE WHERE date_creation BETWEEN ? AND ? "
            + "GROUP BY origine ORDER BY nb DESC";

    private final JdbcTemplate jdbcTemplate;

    public RecapReportRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /** Port of recapclient.xhtml ("ETAT RECAP DES OPERATIONS PAR CLIENT"): grouped by telephone. */
    public List<ReportAggregateRow> recapParClient(Date date1, Date date2) {
        return jdbcTemplate.query(SQL_PAR_CLIENT, RecapReportRepository::mapAggregate, date1, date2);
    }

    /** Port of recapcompte.xhtml ("...PAR COMPTE"): grouped by numero_compte. */
    public List<ReportAggregateRow> recapParCompte(Date date1, Date date2) {
        return jdbcTemplate.query(SQL_PAR_COMPTE, RecapReportRepository::mapAggregate, date1, date2);
    }

    /** Port of recapglobalprdt.xhtml ("...PAR PRODUIT"): grouped by operation type. */
    public List<ReportAggregateRow> recapParProduit(Date date1, Date date2) {
        return jdbcTemplate.query(SQL_PAR_PRODUIT, RecapReportRepository::mapAggregate, date1, date2);
    }

    /** Port of recapglobal.xhtml ("...DES SOUSCRIPTIONS"): grouped by operateur (origine). */
    public List<ReportAggregateRow> recapSouscriptions(Date date1, Date date2) {
        return jdbcTemplate.query(SQL_SOUSCRIPTIONS,
                (rs, rowNum) -> new ReportAggregateRow(rs.getString("label"), rs.getLong("nb"), null, null),
                date1, date2);
    }

    private static ReportAggregateRow mapAggregate(ResultSet rs, int rowNum) throws SQLException {
        return new ReportAggregateRow(
                rs.getString("label"),
                (Number) rs.getObject("nb"),
                (Number) rs.getObject("total_montant"),
                (Number) rs.getObject("total_com"));
    }
}
