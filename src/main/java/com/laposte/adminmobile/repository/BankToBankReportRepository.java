package com.laposte.adminmobile.repository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Faithful JdbcTemplate port of the legacy "Bank To Bank" Jasper reports (screens
 * {@code bb.xhtml}/{@code bbmoov.xhtml}, backed by {@code EtatbeanCtrl.etatbbexcel/etatbbfloozexcel}
 * etc). Unlike most of the other "etats" screens in this module, the SQL for these two reports was
 * recoverable from the legacy source tree ({@code web/state/banbankxls.jrxml} for TMoney,
 * {@code web/state/banbankfloozxls.jrxml} for FLooz - the compiled {@code .jasper} binaries for
 * every other report in the module were not accompanied by their {@code .jrxml} source, so their
 * queries could not be recovered; see {@code TransactionEtatController} for how those are
 * approximated instead). The two queries below are reproduced as closely as possible from that
 * source, with the {@code ?}/{@code $P{...}} placeholders bound as JdbcTemplate parameters.
 * <p>
 * There is no JPA entity for {@code spt.mouvement}/{@code spt.compte}/{@code spt.client} in this
 * port (cross-schema tables outside the 18 modeled entities), hence the direct JdbcTemplate use
 * per the module instructions.
 */
@Repository
public class BankToBankReportRepository {

    private static final SimpleDateFormat YYYYMMDD = new SimpleDateFormat("yyyyMMdd");

    /** From web/state/banbankxls.jrxml - TMoney bank-to-bank movements. */
    private static final String TMONEY_SQL =
            "select mv.numero_compte as numero_compte, montant_mvt as montant, '228'||cl.telephone as telephone, "
            + "r.numero_dest as beneficiaire, "
            + "(case mv.code_operation "
            + "when 23052 then 'ECO-T' "
            + "when 23048 then 'ECO-ECO' "
            + "when 23054 then 'T-ECO' "
            + "when 23050 then 'ECO-BT' "
            + "end) as typeop, "
            + "date_mvt as date_operation, mv.code_operation as code_operation, r.numero_operation as numero_operation, "
            + "c.intitule as intitule "
            + "from spt.mouvement mv, spt.compte c, spt.client cl, spt.recu_electronique r "
            + "where mv.numero_compte = c.numero_compte "
            + "and c.numero_client = cl.numero_client "
            + "and r.numero_operation = mv.numero_operation "
            + "and mv.numero_compte like '990%' "
            + "and mv.code_operation in (23048,23052,23054,23050,23154) "
            + "and to_number(to_char(mv.date_creation,'yyyymmdd')) between ? and ? "
            + "and montant_mvt > 0 "
            + "and sens = 'D' "
            + "order by mv.date_creation asc";

    /** From web/state/banbankfloozxls.jrxml - FLooz bank-to-bank movements. */
    private static final String FLOOZ_SQL =
            "select r.refbank as refbank, mv.numero_compte as numero_compte, montant_mvt as montant, "
            + "'228'||cl.telephone as telephone, r.numero_dest as beneficiaire, "
            + "(case mv.code_operation "
            + "when 23152 then 'ECO-T' "
            + "when 23148 then 'ECO-ECO' "
            + "when 23154 then 'ECO-T-T' "
            + "end) as typeop, "
            + "date_mvt as date_operation, mv.code_operation as code_operation, r.numero_operation as numero_operation, "
            + "c.intitule as intitule "
            + "from spt.mouvement mv, spt.compte c, spt.client cl, spt.recu_electronique r "
            + "where mv.numero_compte = c.numero_compte "
            + "and c.numero_client = cl.numero_client "
            + "and r.numero_operation = mv.numero_operation "
            + "and mv.code_operation in (23148,23152,23154,23054,23052) "
            + "and to_number(to_char(mv.date_creation,'yyyymmdd')) between ? and ? "
            + "and montant_mvt > 0 "
            + "and sens = 'D' "
            + "and length(refbank) >= 13 "
            + "order by mv.date_creation asc";

    private final JdbcTemplate jdbcTemplate;

    public BankToBankReportRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * @param operateur "TMoney" or "FLooz" (the legacy screen's only two {@code f:selectItem}
     *                  choices); anything else is treated as TMoney, matching the legacy
     *                  {@code "FLooz".equals(operateur)} / else-TMoney branching in EtatbeanCtrl.
     */
    public List<BankToBankRow> findBankToBank(Date date1, Date date2, String operateur) {
        boolean flooz = "FLooz".equalsIgnoreCase(operateur);
        String sql = flooz ? FLOOZ_SQL : TMONEY_SQL;
        long d1 = Long.parseLong(YYYYMMDD.format(date1));
        long d2 = Long.parseLong(YYYYMMDD.format(date2));
        return jdbcTemplate.query(sql, (rs, rowNum) -> new BankToBankRow(
                flooz ? rs.getString("refbank") : null,
                rs.getBigDecimal("numero_compte"),
                rs.getBigDecimal("montant"),
                rs.getString("telephone"),
                rs.getString("beneficiaire"),
                rs.getString("typeop"),
                rs.getTimestamp("date_operation"),
                rs.getBigDecimal("code_operation"),
                rs.getBigDecimal("numero_operation"),
                rs.getString("intitule")
        ), d1, d2);
    }
}
