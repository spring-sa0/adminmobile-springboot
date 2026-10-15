package com.laposte.adminmobile.export;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Export Excel (.xlsx) d'une liste : titre, criteres de recherche, date d'export, puis le tableau
 * avec en-tete fige et filtres automatiques. Ecriture en flux (SXSSF) : seules 200 lignes sont
 * gardees en memoire a la fois.
 */
public final class ExportExcel {

    /** Entiers convertis en nombres ; au-dela de 12 chiffres (numeros de compte...) on garde du texte. */
    private static final Pattern ENTIER = Pattern.compile("-?(0|[1-9]\\d{0,11})");
    private static final Pattern DECIMAL = Pattern.compile("-?(0|[1-9]\\d{0,11})[.,]\\d{1,6}");
    private static final int LARGEUR_MAX = 60;

    private ExportExcel() {
    }

    public static void ecrire(TableauExport tableau, String pied, OutputStream out) throws IOException {
        SXSSFWorkbook classeur = new SXSSFWorkbook(200);
        try {
            SXSSFSheet feuille = classeur.createSheet(nomFeuille(tableau.titre()));
            int nbColonnes = Math.max(1, tableau.entetes().size());

            Font gras = classeur.createFont();
            gras.setBold(true);
            Font titreFont = classeur.createFont();
            titreFont.setBold(true);
            titreFont.setFontHeightInPoints((short) 14);

            CellStyle styleTitre = classeur.createCellStyle();
            styleTitre.setFont(titreFont);
            CellStyle styleEntete = classeur.createCellStyle();
            styleEntete.setFont(gras);
            styleEntete.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            styleEntete.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleEntete.setBorderBottom(BorderStyle.THIN);
            styleEntete.setWrapText(true);
            CellStyle styleEntier = classeur.createCellStyle();
            styleEntier.setDataFormat(classeur.createDataFormat().getFormat("0"));
            // Deux formats : "#,##0.##" afficherait "1 500," (separateur decimal orphelin) pour un entier.
            CellStyle styleMontant = classeur.createCellStyle();
            styleMontant.setDataFormat(classeur.createDataFormat().getFormat("#,##0"));
            CellStyle styleMontantDecimal = classeur.createCellStyle();
            styleMontantDecimal.setDataFormat(classeur.createDataFormat().getFormat("#,##0.00"));

            int r = 0;
            cellule(feuille.createRow(r++), 0, tableau.titre()).setCellStyle(styleTitre);
            for (String critere : tableau.criteres()) {
                cellule(feuille.createRow(r++), 0, critere);
            }
            cellule(feuille.createRow(r++), 0, pied);
            r++;

            int ligneEntete = r;
            Row entete = feuille.createRow(r++);
            int[] largeurs = new int[nbColonnes];
            boolean[] montant = new boolean[nbColonnes];
            for (int c = 0; c < tableau.entetes().size(); c++) {
                String libelle = tableau.entetes().get(c);
                cellule(entete, c, libelle).setCellStyle(styleEntete);
                largeurs[c] = libelle.length();
                montant[c] = Colonnes.estMontant(libelle);
            }

            for (List<String> ligne : tableau.lignes()) {
                Row row = feuille.createRow(r++);
                for (int c = 0; c < ligne.size() && c < nbColonnes; c++) {
                    String v = ligne.get(c);
                    Cell cell = row.createCell(c);
                    if (ENTIER.matcher(v).matches()) {
                        cell.setCellValue(Long.parseLong(v));
                        cell.setCellStyle(montant[c] ? styleMontant : styleEntier);
                    } else if (montant[c] && DECIMAL.matcher(v).matches()) {
                        cell.setCellValue(Double.parseDouble(v.replace(',', '.')));
                        cell.setCellStyle(styleMontantDecimal);
                    } else {
                        cell.setCellValue(v);
                    }
                    largeurs[c] = Math.max(largeurs[c], v.length());
                }
            }

            for (int c = 0; c < nbColonnes; c++) {
                feuille.setColumnWidth(c, (Math.min(largeurs[c], LARGEUR_MAX) + 3) * 256);
            }
            feuille.createFreezePane(0, ligneEntete + 1);
            if (!tableau.entetes().isEmpty()) {
                feuille.setAutoFilter(new CellRangeAddress(ligneEntete, Math.max(ligneEntete, r - 1), 0, nbColonnes - 1));
            }
            classeur.write(out);
        } finally {
            classeur.close();
        }
    }

    private static Cell cellule(Row row, int colonne, String valeur) {
        Cell cell = row.createCell(colonne);
        cell.setCellValue(valeur);
        return cell;
    }

    /** Nom d'onglet Excel : 31 caracteres maximum, sans : \ / ? * [ ]. */
    private static String nomFeuille(String titre) {
        String nom = titre.replaceAll("[:\\\\/?*\\[\\]]", " ").trim();
        if (nom.isEmpty()) {
            nom = "Export";
        }
        return nom.length() > 31 ? nom.substring(0, 31) : nom;
    }
}
