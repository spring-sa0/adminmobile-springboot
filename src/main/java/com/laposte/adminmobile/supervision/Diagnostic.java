package com.laposte.adminmobile.supervision;

import java.util.List;

/**
 * Explication d'une erreur pour l'exploitant : ce qui se passe, pourquoi, et quoi faire.
 *
 * @param code      identifiant stable de la regle (ex. ORACLE_MOT_DE_PASSE_EXPIRE)
 * @param titre     resume en une ligne
 * @param gravite   impact sur le service
 * @param categorie domaine concerne (Base de donnees, Configuration, Memoire...)
 * @param cause     cause probable
 * @param actions   etapes de resolution, dans l'ordre
 */
public record Diagnostic(String code, String titre, Gravite gravite, String categorie, String cause, List<String> actions) {

    public enum Gravite {
        /** Service interrompu ou sur le point de l'etre pour tous les utilisateurs. */
        CRITIQUE,
        /** Une fonctionnalite est en panne. */
        ELEVEE,
        /** Erreur ponctuelle ou limitee a certains cas. */
        MOYENNE,
        /** Sans impact reel (ex. utilisateur ayant ferme son navigateur). */
        FAIBLE
    }
}
