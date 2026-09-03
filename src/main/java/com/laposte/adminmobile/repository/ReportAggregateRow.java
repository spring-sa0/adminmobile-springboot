package com.laposte.adminmobile.repository;

/**
 * Generic "group by X, count, sum(montant), sum(commission)" row, shared by the JdbcTemplate
 * aggregate queries in {@link RecapReportRepository}, {@link CommissionsReportRepository} and
 * {@link MarchandReportRepository}. {@code label} is computed in SQL (telephone, account number,
 * operation name, operateur, ...) so one row shape covers every "recap" screen in the Comptes/
 * Marchand/Recap/Commissions module without a dedicated DTO per query.
 * <p>
 * Fields are {@link Number} rather than a fixed numeric type because the exact JDBC numeric type
 * Oracle returns for {@code COUNT}/{@code SUM} varies (BigDecimal in practice); Thymeleaf's
 * {@code #numbers} utility and plain {@code th:text} both work fine against {@link Number}.
 */
public class ReportAggregateRow {

    private final String label;
    private final Number count;
    private final Number totalMontant;
    private final Number totalCommission;

    public ReportAggregateRow(String label, Number count, Number totalMontant, Number totalCommission) {
        this.label = label;
        this.count = count;
        this.totalMontant = totalMontant;
        this.totalCommission = totalCommission;
    }

    public String getLabel() {
        return label;
    }

    public Number getCount() {
        return count;
    }

    public Number getTotalMontant() {
        return totalMontant;
    }

    public Number getTotalCommission() {
        return totalCommission;
    }
}
