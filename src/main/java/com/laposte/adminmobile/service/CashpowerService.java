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

    private static String libelle(String nomPrenom, String compteur) {
        String l = ((nomPrenom == null ? "" : nomPrenom) + " " + (compteur == null ? "" : compteur)).trim();
        return l.length() > 200 ? l.substring(0, 200) : l;
    }

    /** Ligne de SPT.CASHPOWER affichee avant annulation. */
    public record OperationCashpower(long numeroOperation, String libelle, BigDecimal montant, Date dateAchat,
                                     String telephone, String numeroCompte, String numeroCompteur, String statut,
                                     String numeroRecu, String kilowatt) {
    }

    /** Retour de PROCEDURES.P_ANNULER_OPERATION. */
    public record ResultatAnnulation(int code, String message) {
        public boolean reussi() {
            return code == 0;
        }
    }
}
