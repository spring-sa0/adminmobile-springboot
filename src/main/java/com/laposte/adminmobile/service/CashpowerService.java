package com.laposte.adminmobile.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Annulation des operations CashPower (achats d'electricite prepayee), portage du formulaire
 * Oracle Forms {@code annul_op.fmb} ("ANNULATION DES OPERATIONS").
 * <p>
 * Comme le formulaire : recherche de l'operation dans {@code SPT.CASHPOWER}, puis appel de
 * {@code PROCEDURES.P_ANNULER_OPERATION(numero, utilisateur, code_retour OUT, message_retour OUT)}.
 * La procedure passe les ecritures inverses dans {@code SPT.MOUVEMENT} (libelle_ecriture
 * "ANNULATION OP n"), fait elle-meme son COMMIT, et renvoie :
 * <ul>
 *   <li>0 : annulation effectuee (le message donne le numero d'annulation) ;</li>
 *   <li>1 : aucune ecriture pour cette operation, rien n'est modifie ;</li>
 *   <li>2 : operation deja annulee ;</li>
 *   <li>-1 : erreur technique (ROLLBACK).</li>
 * </ul>
 * Pas de transaction Spring autour de l'appel : la procedure valide elle-meme.
 */
@Service
public class CashpowerService {

    private static final String SQL_OPERATION =
            "SELECT NUMERO_OPERATION, NOMPRENOM, NUMEROCOMPTEUR, MONTANT, DATE_ACHAT, TELEPHONE, NUMERO_COMPTE, "
                    + "STATUT, NUMERO_RECU, KILOWATEUR FROM SPT.CASHPOWER WHERE NUMERO_OPERATION = ? ORDER BY DATE_ACHAT DESC";

    /** Meme test que la procedure (etape 2) pour signaler une annulation existante avant de proposer le bouton. */
    private static final String SQL_ANNULATION_EXISTANTE =
            "SELECT MAX(NUMERO_OPERATION) FROM SPT.MOUVEMENT WHERE LIBELLE_ECRITURE = ?";

    private static final String APPEL_ANNULATION = "{ call PROCEDURES.P_ANNULER_OPERATION(?, ?, ?, ?) }";

    /**
     * Annulations passees par P_ANNULER_OPERATION sur une periode : une ligne par annulation
     * (la procedure insere une ecriture inverse par ecriture de l'operation d'origine, toutes avec
     * le libelle_ecriture "ANNULATION OP n" et DATE_MVT = date de l'annulation).
     * <p>
     * SPT.MOUVEMENT compte plus de 200 millions de lignes : le filtre sur DATE_MVT (indexee) doit
     * rester le premier critere. Mesure : ~4 s pour 30 jours, ~12 s pour 92 jours (limite des
     * recherches).
     * <p>
     * Le libelle et le montant affiches sont ceux de l'achat CashPower quand l'operation annulee en
     * est un, sinon ceux de l'ecriture principale (montant le plus eleve) de l'annulation.
     */
    private static final String SQL_ANNULATIONS =
            "WITH annul AS ("
                    + " SELECT a.numero_operation AS num_annulation,"
                    + "        MIN(a.date_mvt) AS date_annulation,"
                    + "        MAX(a.code_utilisateur) AS agent,"
                    + "        MAX(a.code_operation) AS code_operation,"
                    + "        TO_NUMBER(REGEXP_SUBSTR(MAX(a.libelle_ecriture), '[0-9]+$')) AS num_origine,"
                    + "        MAX(a.montant_mvt) AS montant_principal,"
                    + "        MAX(REPLACE(a.libelle_mvt, 'ANNULATION : ', '')) KEEP (DENSE_RANK LAST ORDER BY a.montant_mvt) AS libelle_principal,"
                    + "        COUNT(*) AS nb_ecritures"
                    + "   FROM spt.mouvement a"
                    + "  WHERE a.date_mvt BETWEEN ? AND ?"
                    + "    AND a.libelle_ecriture LIKE 'ANNULATION OP %'"
                    + "  GROUP BY a.numero_operation),"
                    // Quelques numeros d'operation existent en plusieurs achats CashPower : on garde le plus recent.
                    + " cp AS (SELECT numero_operation, nomprenom, numerocompteur, montant, telephone,"
                    + "               ROW_NUMBER() OVER (PARTITION BY numero_operation ORDER BY date_achat DESC) AS rn"
                    + "          FROM spt.cashpower)"
                    + " SELECT annul.*, cp.nomprenom, cp.numerocompteur, cp.montant AS montant_cashpower, cp.telephone,"
                    + "        CASE WHEN cp.numero_operation IS NULL THEN 0 ELSE 1 END AS est_cashpower,"
                    // Operation annulee qui etait elle-meme une annulation (index MVT_NUMOP) : a signaler.
                    + "        CASE WHEN EXISTS (SELECT 1 FROM spt.mouvement m2 WHERE m2.numero_operation = annul.num_origine"
                    + "                            AND m2.libelle_ecriture LIKE 'ANNULATION OP %') THEN 1 ELSE 0 END AS annule_une_annulation"
                    + "   FROM annul LEFT JOIN cp ON cp.numero_operation = annul.num_origine AND cp.rn = 1"
                    + "  WHERE (? = 'TOUT' OR (? = 'CASHPOWER' AND cp.numero_operation IS NOT NULL)"
                    + "         OR (? = 'AUTRE' AND cp.numero_operation IS NULL))"
                    + "  ORDER BY annul.date_annulation DESC";

    private final JdbcTemplate jdbcTemplate;

    public CashpowerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Operation(s) CashPower portant ce numero (quelques numeros existent en plusieurs lignes). */
    public List<OperationCashpower> rechercher(long numeroOperation) {
        return jdbcTemplate.query(SQL_OPERATION, (rs, i) -> new OperationCashpower(
                rs.getLong("NUMERO_OPERATION"),
                // Libelle affiche par le formulaire : nom du client + numero de compteur.
                libelle(rs.getString("NOMPRENOM"), rs.getString("NUMEROCOMPTEUR")),
                rs.getBigDecimal("MONTANT"),
                rs.getTimestamp("DATE_ACHAT"),
                rs.getString("TELEPHONE"),
                rs.getString("NUMERO_COMPTE"),
                rs.getString("NUMEROCOMPTEUR"),
                rs.getString("STATUT"),
                rs.getString("NUMERO_RECU"),
                rs.getString("KILOWATEUR")), numeroOperation);
    }

    /** Numero de l'operation d'annulation si elle a deja ete annulee, sinon null. */
    public Long annulationExistante(long numeroOperation) {
        return jdbcTemplate.queryForObject(SQL_ANNULATION_EXISTANTE, Long.class, "ANNULATION OP " + numeroOperation);
    }

    /**
     * Appelle la procedure d'annulation. {@code utilisateur} est enregistre comme auteur des
     * ecritures (CODE_UTILISATEUR), en majuscules comme le {@code USER} Oracle du formulaire.
     */
    public ResultatAnnulation annuler(long numeroOperation, String utilisateur) {
        return jdbcTemplate.execute(APPEL_ANNULATION, (CallableStatement cs) -> {
            cs.setLong(1, numeroOperation);
            cs.setString(2, utilisateur == null ? null : utilisateur.toUpperCase(Locale.ROOT));
            cs.registerOutParameter(3, Types.NUMERIC);
            cs.registerOutParameter(4, Types.VARCHAR);
            cs.execute();
            BigDecimal code = cs.getBigDecimal(3);
            return new ResultatAnnulation(code == null ? -1 : code.intValue(), cs.getString(4));
        });
    }

    /** Filtre de la liste des annulations. */
    public enum TypeAnnulation { TOUT, CASHPOWER, AUTRE }

    /**
     * Annulations faites avec P_ANNULER_OPERATION (formulaire annul_op ou ecran web) entre
     * {@code debut} et {@code fin} inclus, les plus recentes d'abord.
     */
    public List<Annulation> annulations(Date debut, Date fin, TypeAnnulation type) {
        String filtre = (type == null ? TypeAnnulation.TOUT : type).name();
        return jdbcTemplate.query(SQL_ANNULATIONS, (rs, i) -> {
            boolean cashpower = rs.getInt("EST_CASHPOWER") == 1;
            String nature = rs.getInt("ANNULE_UNE_ANNULATION") == 1 ? "Annulation d'une annulation"
                    : cashpower ? "CashPower" : "Autre (code " + rs.getString("CODE_OPERATION") + ")";
            return new Annulation(
                    rs.getTimestamp("DATE_ANNULATION"),
                    rs.getLong("NUM_ANNULATION"),
                    rs.getObject("NUM_ORIGINE") == null ? null : rs.getLong("NUM_ORIGINE"),
                    nature,
                    cashpower ? libelle(rs.getString("NOMPRENOM"), rs.getString("NUMEROCOMPTEUR")) : rs.getString("LIBELLE_PRINCIPAL"),
                    cashpower ? rs.getBigDecimal("MONTANT_CASHPOWER") : rs.getBigDecimal("MONTANT_PRINCIPAL"),
                    rs.getString("TELEPHONE"),
                    rs.getString("AGENT"),
                    rs.getInt("NB_ECRITURES"));
        }, new java.sql.Timestamp(debut.getTime()), new java.sql.Timestamp(fin.getTime()), filtre, filtre, filtre);
    }

    private static String libelle(String nomPrenom, String compteur) {
        String l = ((nomPrenom == null ? "" : nomPrenom) + " " + (compteur == null ? "" : compteur)).trim();
        return l.length() > 200 ? l.substring(0, 200) : l;
    }

    /** Ligne de SPT.CASHPOWER affichee avant annulation. */
    public record OperationCashpower(long numeroOperation, String libelle, BigDecimal montant, Date dateAchat,
                                     String telephone, String numeroCompte, String numeroCompteur, String statut,
                                     String numeroRecu, String kilowatt) {
    }

    /** Une annulation passee par P_ANNULER_OPERATION. */
    public record Annulation(Date dateAnnulation, long numeroAnnulation, Long numeroOperationAnnulee, String type,
                             String libelle, BigDecimal montant, String telephone, String agent, int nbEcritures) {
    }

    /** Retour de PROCEDURES.P_ANNULER_OPERATION. */
    public record ResultatAnnulation(int code, String message) {
        public boolean reussi() {
            return code == 0;
        }
    }
}
