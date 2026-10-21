package com.laposte.adminmobile.service;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Empeche de lancer deux fois en meme temps un traitement sur la meme operation (annulation,
 * restauration). Les procedures d'annulation durent 30 a 60 s et n'ont aucune protection contre
 * deux executions simultanees : un double clic pendant ce temps a laisse une operation a moitie
 * annulee (operation 103952083, 21/10/2026).
 * <p>
 * Verrou en memoire : suffisant pour l'instance unique de l'application. Il ne protege pas d'un
 * autre programme (ancienne application, Forms) qui lancerait la meme procedure.
 */
@Component
public class OperationsEnCours {

    private final Set<String> enCours = ConcurrentHashMap.newKeySet();

    /** Reserve le traitement ; faux s'il est deja en cours (ne rien faire dans ce cas). */
    public boolean demarrer(String traitement, long numeroOperation) {
        return enCours.add(cle(traitement, numeroOperation));
    }

    /** A appeler dans un finally apres un {@link #demarrer} reussi. */
    public void terminer(String traitement, long numeroOperation) {
        enCours.remove(cle(traitement, numeroOperation));
    }

    private static String cle(String traitement, long numeroOperation) {
        return traitement + ":" + numeroOperation;
    }
}
