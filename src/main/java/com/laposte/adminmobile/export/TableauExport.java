package com.laposte.adminmobile.export;

import java.util.List;

/**
 * Contenu d'une liste a exporter, tel qu'affiche a l'ecran.
 *
 * @param titre     titre de l'ecran (h2)
 * @param criteres  criteres de recherche saisis, deja mis en forme ("Date debut : 2026-01-01")
 * @param entetes   intitules des colonnes exportees
 * @param lignes    valeurs, une liste par ligne, alignees sur {@code entetes}
 */
public record TableauExport(String titre, List<String> criteres, List<String> entetes, List<List<String>> lignes) {
}
