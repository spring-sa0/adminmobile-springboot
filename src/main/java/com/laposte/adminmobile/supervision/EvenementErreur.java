package com.laposte.adminmobile.supervision;

/**
 * Une erreur journalisee par l'application, avec son contexte.
 *
 * @param horodatage     instant (millisecondes epoch)
 * @param niveau         ERROR ou WARN
 * @param logger         classe qui a journalise
 * @param message        message journalise
 * @param exception      classe de l'exception journalisee (ou null)
 * @param causeRacine    classe de la derniere exception de la chaine (ou null)
 * @param messageCause   message de la cause racine
 * @param ligneCode      premiere ligne de code de l'application dans la trace (com.laposte...)
 * @param trace          trace complete (tronquee)
 * @param reference      reference montree a l'utilisateur sur la page 500 (ou null)
 * @param url            URL de la requete en cours (ou null hors requete)
 * @param utilisateur    utilisateur connecte (ou null)
 * @param signature      cle de regroupement (meme cause = meme signature)
 */
public record EvenementErreur(long horodatage, String niveau, String logger, String message, String exception,
                              String causeRacine, String messageCause, String ligneCode, String trace,
                              String reference, String url, String utilisateur, String signature) {

    /** Horodatage sous forme de date, pour l'affichage. */
    public java.util.Date date() {
        return new java.util.Date(horodatage);
    }
}
