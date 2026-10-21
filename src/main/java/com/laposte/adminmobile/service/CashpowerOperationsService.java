package com.laposte.adminmobile.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Achats CashPower (electricite prepayee) faits depuis les applications : table {@code SPT.CASHPOWER}
 * (partie "EPOSTE" de la requete de suivi CashPower ; les achats au guichet, tables
 * ENCAISSEMENTS_CASH_POWER*, ne sont pas concernes).
 * <p>
 * Valeurs rencontrees en base (octobre 2026) :
 * <ul>
 *   <li>APP_SOURCE : EPOSTE, MARCHAND, OMNIPOST, et vide pour les achats d'avant fin juillet 2025 ;</li>
 *   <li>STATUT : SUCCESS, FAILED, PENDING, INITIATED, et vide pour les memes achats anciens ;
 *       REMBOURSE pour un achat annule ({@link CashpowerService#STATUT_REMBOURSE}).</li>
 * </ul>
 * La liste est classee par application (E-Poste, Marchand, autres) puis par statut puis par date.
 * La table est petite (~50 000 lignes, pas d'index sur DATE_ACHAT) : parcours complet de quelques
 * dizaines de millisecondes.
 */
@Service
public class CashpowerOperationsService {

    private static final String SQL =
            "SELECT DATE_ACHAT, APP_SOURCE, STATUT, NUMERO_OPERATION, NOMPRENOM, TELEPHONE, NUMEROCOMPTEUR,"
                    + " MONTANT, KILOWATEUR, CODERECHARGE, AUTRE_INFOS"
                    + " FROM SPT.CASHPOWER WHERE DATE_ACHAT BETWEEN ? AND ?";

    private static final String TRI =
            " ORDER BY CASE APP_SOURCE WHEN 'EPOSTE' THEN 1 WHEN 'MARCHAND' THEN 2 ELSE 3 END,"
                    + " CASE STATUT WHEN 'SUCCESS' THEN 1 WHEN 'FAILED' THEN 2 WHEN 'PENDING' THEN 3"
                    + " WHEN 'INITIATED' THEN 4 WHEN 'REMBOURSE' THEN 5 ELSE 6 END, DATE_ACHAT";

    /** Application d'origine de l'achat. */
    public enum Application {
        TOUTES(""),
        EPOSTE(" AND APP_SOURCE = 'EPOSTE'"),
        MARCHAND(" AND APP_SOURCE = 'MARCHAND'"),
        /** OmniPost et achats anciens sans application renseignee. */
        AUTRES(" AND (APP_SOURCE IS NULL OR APP_SOURCE NOT IN ('EPOSTE', 'MARCHAND'))");

        private final String condition;

        Application(String condition) {
            this.condition = condition;
        }
    }

    /** Filtre sur le statut de l'achat. */
    public enum Statut {
        TOUS(""),
        ABOUTIES(" AND STATUT = 'SUCCESS'"),
        /** Echouees, en attente, initiees : tout statut renseigne autre que SUCCESS et REMBOURSE. */
        NON_ABOUTIES(" AND STATUT NOT IN ('SUCCESS', 'REMBOURSE')"),
        ECHOUEES(" AND STATUT = 'FAILED'"),
        EN_ATTENTE(" AND STATUT = 'PENDING'"),
        INITIEES(" AND STATUT = 'INITIATED'"),
        REMBOURSEES(" AND STATUT = 'REMBOURSE'"),
        NON_RENSEIGNE(" AND STATUT IS NULL");

        private final String condition;

        Statut(String condition) {
            this.condition = condition;
        }
    }

    private final JdbcTemplate jdbcTemplate;

    public CashpowerOperationsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Achats entre {@code debut} et {@code fin} inclus, classes par application, statut puis date. */
    public List<AchatCashpower> operations(Date debut, Date fin, Application application, Statut statut) {
        String sql = SQL + (application == null ? Application.TOUTES : application).condition
                + (statut == null ? Statut.TOUS : statut).condition + TRI;
        return jdbcTemplate.query(sql, (rs, i) -> new AchatCashpower(
                rs.getTimestamp("DATE_ACHAT"),
                libelleApplication(rs.getString("APP_SOURCE")),
                libelleStatut(rs.getString("STATUT")),
                rs.getObject("NUMERO_OPERATION") == null ? null : rs.getLong("NUMERO_OPERATION"),
                rs.getString("NOMPRENOM"),
                rs.getString("TELEPHONE"),
                rs.getString("NUMEROCOMPTEUR"),
                rs.getBigDecimal("MONTANT"),
                rs.getString("KILOWATEUR"),
                rs.getString("CODERECHARGE"),
                rs.getString("AUTRE_INFOS")),
                new Timestamp(debut.getTime()), new Timestamp(fin.getTime()));
    }

    /** Nombre et montant par application et par statut, dans l'ordre de la liste. */
    public static List<LigneRecap> recapitulatif(List<AchatCashpower> achats) {
        Map<String, LigneRecap> lignes = new LinkedHashMap<>();
        for (AchatCashpower a : achats) {
            String cle = a.application() + "|" + a.statut();
            LigneRecap l = lignes.get(cle);
            BigDecimal montant = a.montant() == null ? BigDecimal.ZERO : a.montant();
            lignes.put(cle, l == null ? new LigneRecap(a.application(), a.statut(), 1, montant)
                    : new LigneRecap(l.application(), l.statut(), l.nombre() + 1, l.montant().add(montant)));
        }
        return new ArrayList<>(lignes.values());
    }

    static String libelleApplication(String appSource) {
        if (appSource == null) {
            return "Non renseignee";
        }
        return switch (appSource) {
            case "EPOSTE" -> "E-Poste";
            case "MARCHAND" -> "Marchand";
            case "OMNIPOST" -> "OmniPost";
            default -> appSource;
        };
    }

    static String libelleStatut(String statut) {
        if (statut == null) {
            return "Non renseigne";
        }
        return switch (statut) {
            case "SUCCESS" -> "Aboutie";
            case "FAILED" -> "Echouee";
            case "PENDING" -> "En attente";
            case "INITIATED" -> "Initiee";
            case "REMBOURSE" -> "Remboursee";
            default -> statut;
        };
    }

    /** Une ligne de SPT.CASHPOWER. */
    public record AchatCashpower(Date dateAchat, String application, String statut, Long numeroOperation,
                                 String client, String telephone, String compteur, BigDecimal montant,
                                 String kwh, String jeton, String recu) {
    }

    /** Total d'un couple application / statut. */
    public record LigneRecap(String application, String statut, int nombre, BigDecimal montant) {
    }
}
