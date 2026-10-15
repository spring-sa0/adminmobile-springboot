package com.laposte.adminmobile.web;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Regles communes de saisie des numeros de telephone et de compte, appliquees a tous les ecrans
 * par {@link SaisieNormalisationFilter} (mise en forme) et {@link SaisieValidationInterceptor}
 * (refus d'une valeur invalide avec un message clair).
 * <p>
 * Formats constates en base : USERS_EPOSTE.TELEPHONE sur 8 chiffres, SPT.RECU_ELECTRONIQUE.TELEPHONE
 * avec l'indicatif (228 + 8 chiffres), SPT.COMPTE.NUMERO_COMPTE de 1 a 16 chiffres. Les ecrans
 * attendent le numero sur 8 chiffres et ajoutent eux-memes 228 pour chercher dans les operations.
 */
final class Saisies {

    static final Set<String> PARAMS_TELEPHONE = Set.of("telephone", "ptelephone");
    static final Set<String> PARAMS_COMPTE = Set.of("numcompte", "numerocompte", "numeroCompte");

    static final String MESSAGE_TELEPHONE = "Numero de telephone invalide : 8 chiffres attendus (ex. 90123456).";
    static final String MESSAGE_COMPTE = "Numero de compte invalide : chiffres uniquement (16 au maximum).";

    private static final Pattern SEPARATEURS = Pattern.compile("[\\s.\\-()/]");
    private static final Pattern TELEPHONE = Pattern.compile("\\d{8}");
    private static final Pattern COMPTE = Pattern.compile("\\d{1,16}");

    private Saisies() {
    }

    /**
     * "+228 90 12 34 56", "00228 90123456", "22890123456", "90.12.34.56" -> "90123456". Une valeur
     * qui ne se ramene pas a 8 chiffres est rendue telle quelle (sans separateurs) : c'est la
     * validation qui la refuse.
     */
    static String normaliserTelephone(String saisie) {
        if (saisie == null) {
            return null;
        }
        String s = SEPARATEURS.matcher(saisie).replaceAll("");
        if (s.startsWith("+")) {
            s = s.substring(1);
        }
        if (s.length() == 13 && s.startsWith("00228")) {
            s = s.substring(5);
        } else if (s.length() == 11 && s.startsWith("228")) {
            s = s.substring(3);
        }
        return s;
    }

    static String normaliserCompte(String saisie) {
        return saisie == null ? null : SEPARATEURS.matcher(saisie).replaceAll("");
    }

    static boolean telephoneValide(String valeur) {
        return valeur == null || valeur.isEmpty() || TELEPHONE.matcher(valeur).matches();
    }

    static boolean compteValide(String valeur) {
        return valeur == null || valeur.isEmpty() || COMPTE.matcher(valeur).matches();
    }
}
