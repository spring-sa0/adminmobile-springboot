package com.laposte.adminmobile.supervision;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Erreurs de meme cause (meme signature) : compteur, premiere / derniere occurrence, diagnostic
 * et dernieres occurrences detaillees.
 */
public final class GroupeErreurs {

    /** Occurrences detaillees conservees par groupe. */
    static final int OCCURRENCES_MAX = 20;

    private final String id;
    private final String signature;
    private final Diagnostic diagnostic;
    private final long premiere;
    private long derniere;
    private long nombre;
    private final Deque<EvenementErreur> recentes = new ArrayDeque<>();
    private long resoluLe;
    private String resoluPar;
    /** Occurrences survenues depuis la derniere resolution (0 = toujours resolue). */
    private long occurrencesDepuisResolution;

    GroupeErreurs(String id, EvenementErreur premier, Diagnostic diagnostic) {
        this.id = id;
        this.signature = premier.signature();
        this.diagnostic = diagnostic;
        this.premiere = premier.horodatage();
        this.derniere = premier.horodatage();
    }

    synchronized void ajouter(EvenementErreur e) {
        nombre++;
        if (resoluLe > 0) {
            occurrencesDepuisResolution++;
        }
        derniere = Math.max(derniere, e.horodatage());
        recentes.addFirst(e);
        while (recentes.size() > OCCURRENCES_MAX) {
            recentes.removeLast();
        }
    }

    public String getId() {
        return id;
    }

    public String getSignature() {
        return signature;
    }

    public Diagnostic getDiagnostic() {
        return diagnostic;
    }

    public long getPremiere() {
        return premiere;
    }

    public synchronized long getDerniere() {
        return derniere;
    }

    public synchronized long getNombre() {
        return nombre;
    }

    /** Occurrences les plus recentes d'abord. */
    public synchronized List<EvenementErreur> getRecentes() {
        return new ArrayList<>(recentes);
    }

    public synchronized EvenementErreur getDerniereOccurrence() {
        return recentes.peekFirst();
    }

    public java.util.Date getPremiereDate() {
        return new java.util.Date(premiere);
    }

    public synchronized java.util.Date getDerniereDate() {
        return new java.util.Date(derniere);
    }

    /**
     * Marque la cause comme resolue a l'instant {@code quand}. Au rechargement de l'historique, les
     * occurrences deja chargees et posterieures a {@code quand} comptent comme survenues depuis.
     */
    synchronized void marquerResolu(long quand, String par) {
        this.resoluLe = quand;
        this.resoluPar = par;
        this.occurrencesDepuisResolution = recentes.stream().filter(e -> e.horodatage() > quand).count();
    }

    /** Marquee resolue et aucune occurrence depuis. */
    public synchronized boolean isResolu() {
        return resoluLe > 0 && occurrencesDepuisResolution == 0;
    }

    /** Marquee resolue, mais survenue a nouveau depuis : a reexaminer. */
    public synchronized boolean isReapparue() {
        return resoluLe > 0 && occurrencesDepuisResolution > 0;
    }

    public synchronized java.util.Date getResoluLe() {
        return resoluLe > 0 ? new java.util.Date(resoluLe) : null;
    }

    public synchronized String getResoluPar() {
        return resoluPar;
    }
}
