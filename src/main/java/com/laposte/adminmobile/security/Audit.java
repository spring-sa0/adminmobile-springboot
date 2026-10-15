package com.laposte.adminmobile.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

/**
 * Journal d'audit : une ligne par connexion / echec / deconnexion et par action effectuee dans
 * l'interface (logger "AUDIT", ecrit dans logs/audit.log - voir logback-spring.xml).
 * Format : {@code evenement utilisateur=... ip=... cle=valeur ...}.
 * <p>
 * Les valeurs sont nettoyees (retours a la ligne neutralises, longueur bornee) pour qu'une saisie
 * ne puisse pas fabriquer de fausses lignes dans le journal.
 */
public final class Audit {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
    private static final int LONGUEUR_MAX = 120;

    private Audit() {
    }

    public static void evenement(String evenement, String utilisateur, String ip, Map<String, String> details) {
        StringJoiner ligne = new StringJoiner(" ");
        ligne.add(nettoyer(evenement));
        ligne.add("utilisateur=" + nettoyer(utilisateur));
        ligne.add("ip=" + nettoyer(ip));
        if (details != null) {
            new TreeMap<>(details).forEach((cle, valeur) -> ligne.add(nettoyer(cle) + "=" + guillemets(valeur)));
        }
        AUDIT.info(ligne.toString());
    }

    private static String guillemets(String valeur) {
        String v = nettoyer(valeur);
        return v.indexOf(' ') >= 0 || v.isEmpty() ? "\"" + v.replace("\"", "'") + "\"" : v;
    }

    static String nettoyer(String valeur) {
        if (valeur == null) {
            return "-";
        }
        String v = valeur.replaceAll("[\\r\\n\\t]", " ").trim();
        return v.length() > LONGUEUR_MAX ? v.substring(0, LONGUEUR_MAX) + "..." : v;
    }
}
