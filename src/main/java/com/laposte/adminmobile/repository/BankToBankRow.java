package com.laposte.adminmobile.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * One row of the "Bank To Bank" report (legacy {@code bb.xhtml} / {@code bbmoov.xhtml}), as
 * produced by {@link BankToBankReportRepository}. Field names/shape mirror the columns selected
 * by the legacy Jasper report queries (see {@code web/state/banbankxls.jrxml} and
 * {@code web/state/banbankfloozxls.jrxml} in the legacy source tree).
 */
public record BankToBankRow(
        String refbank,
        BigDecimal numeroCompte,
        BigDecimal montant,
        String telephone,
        String beneficiaire,
        String typeOperation,
        Timestamp dateOperation,
        BigDecimal codeOperation,
        BigDecimal numeroOperation,
        String intitule) {
}
