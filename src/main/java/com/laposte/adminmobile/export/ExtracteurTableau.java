package com.laposte.adminmobile.export;

import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.parser.StreamParser;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Extrait d'une page rendue le tableau de la liste ({@code table.data-table}) : exporter ce qui
 * est affiche garantit des colonnes et un formatage identiques a l'ecran, sans definir les
 * colonnes une seconde fois pour chacun des ecrans.
 * <p>
 * Regles :
 * <ul>
 *   <li>seul le premier {@code table.data-table} est pris, hors tableaux d'aide marques
 *       {@code no-export} (ex. l'intitule de compte de releve-parametrage) ;</li>
 *   <li>une colonne n'est pas exportee si son en-tete est vide, porte la classe {@code no-export},
 *       ou si ses cellules contiennent un formulaire / bouton (colonnes d'action) ;</li>
 *   <li>une ligne faite d'une seule cellule fusionnee ("Il n'y a aucune operation...") est
 *       ignoree.</li>
 * </ul>
 * La page est lue en flux (StreamParser) et chaque ligne est liberee des qu'elle est lue : un
 * export de plusieurs dizaines de milliers de lignes ne construit pas tout l'arbre HTML en memoire.
 */
public final class ExtracteurTableau {

    private ExtracteurTableau() {
    }

    /** Renvoie null si la page ne contient aucune liste exportable. */
    public static TableauExport extraire(String html) {
        String titre = null;
        List<String> criteres = new ArrayList<>();
        List<String> entetes = null;
        BitSet colonnesExclues = new BitSet();
        List<List<String>> lignes = new ArrayList<>();
        Element tableCible = null;

        try (StreamParser parser = new StreamParser(Parser.htmlParser()).parse(html, "")) {
            for (Element el : (Iterable<Element>) parser.stream()::iterator) {
                switch (el.normalName()) {
                    case "h2" -> {
                        if (titre == null) {
                            titre = el.text();
                        }
                    }
                    case "div" -> {
                        if (el.hasClass("form-row") && dansFormulaireDeRecherche(el)) {
                            String critere = critere(el);
                            if (critere != null) {
                                criteres.add(critere);
                            }
                        }
                    }
                    case "tr" -> {
                        Element table = el.closest("table");
                        if (table == null || !exportable(table)) {
                            continue;
                        }
                        if (tableCible == null) {
                            tableCible = table;
                        } else if (table != tableCible) {
                            continue;
                        }
                        if (el.parent() != null && "thead".equals(el.parent().normalName())) {
                            entetes = new ArrayList<>();
                            List<Element> ths = el.children();
                            for (int i = 0; i < ths.size(); i++) {
                                Element th = ths.get(i);
                                if (th.text().isBlank() || th.hasClass("no-export")) {
                                    colonnesExclues.set(i);
                                }
                                entetes.add(th.text());
                            }
                        } else {
                            List<Element> tds = el.children();
                            if (tds.size() == 1 && tds.get(0).hasAttr("colspan")) {
                                el.remove();
                                continue;
                            }
                            List<String> ligne = new ArrayList<>(tds.size());
                            for (int i = 0; i < tds.size(); i++) {
                                Element td = tds.get(i);
                                if (td.hasClass("no-export") || !td.select("form, button, input, select, textarea").isEmpty()) {
                                    colonnesExclues.set(i);
                                }
                                ligne.add(td.text());
                            }
                            lignes.add(ligne);
                            el.remove();
                        }
                    }
                    default -> {
                    }
                }
            }
        }

        if (tableCible == null || entetes == null) {
            return null;
        }
        return new TableauExport(titre == null ? "Export" : titre, criteres,
                garder(entetes, colonnesExclues), lignes.stream().map(l -> garder(l, colonnesExclues)).toList());
    }

    private static boolean exportable(Element table) {
        return table.hasClass("data-table") && !table.hasClass("no-export");
    }

    private static boolean dansFormulaireDeRecherche(Element el) {
        Element form = el.closest("form");
        return form != null && "get".equalsIgnoreCase(form.attr("method"));
    }

    /** "Libelle : valeur" d'un champ du formulaire de recherche, ou null s'il est vide. */
    private static String critere(Element formRow) {
        Element label = formRow.selectFirst("label");
        String valeur = null;
        Element select = formRow.selectFirst("select");
        if (select != null) {
            Element option = select.selectFirst("option[selected]");
            valeur = option != null ? option.text() : null;
        } else {
            Element input = formRow.selectFirst("input:not([type=hidden]):not([type=checkbox]):not([type=radio])");
            valeur = input != null ? input.attr("value") : null;
        }
        if (label == null || valeur == null || valeur.isBlank()) {
            return null;
        }
        return label.text() + " : " + valeur.trim();
    }

    private static List<String> garder(List<String> valeurs, BitSet exclues) {
        List<String> resultat = new ArrayList<>(valeurs.size());
        for (int i = 0; i < valeurs.size(); i++) {
            if (!exclues.get(i)) {
                resultat.add(valeurs.get(i));
            }
        }
        return resultat;
    }
}
