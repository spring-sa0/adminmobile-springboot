package com.laposte.adminmobile.export;

import java.util.regex.Pattern;

/** Regles communes aux exports Excel et PDF sur la nature des colonnes. */
final class Colonnes {

    // Mots entiers : "Com" (commission) mais pas "Compte".
    private static final Pattern MONTANT =
            Pattern.compile("(?iu).*\\b(montants?|soldes?|com|commissions?|frais|totale?s?|tva|taf)\\b.*");

    private Colonnes() {
    }

    /**
     * Colonne de montants, d'apres son intitule : separateur de milliers en Excel, alignement a
     * droite en PDF. Les autres nombres (telephones, numeros d'operation) sont des identifiants.
     */
    static boolean estMontant(String entete) {
        return MONTANT.matcher(entete).matches();
    }
}
