package com.laposte.adminmobile.export;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.ColumnText;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Export PDF d'une liste : A4 paysage, titre, criteres de recherche, tableau avec en-tete repete
 * sur chaque page, pied "Page n" et date d'export. Police Liberation Sans embarquee (la meme que
 * pour les rapports Jasper, voir fonts/fonts.xml) : accents corrects quel que soit le poste.
 */
public final class ExportPdf {

    private static final String POLICE_NORMALE = "fonts/liberation/LiberationSans-Regular.ttf";
    private static final String POLICE_GRASSE = "fonts/liberation/LiberationSans-Bold.ttf";
    private static final Color GRIS_ENTETE = new Color(0xE3, 0xF1, 0xEE);
    private static final Color GRIS_LIGNE = new Color(0xF6, 0xF8, 0xF8);
    private static final Color TEAL = new Color(0x0A, 0x2E, 0x2A);

    private static final BaseFont NORMALE = police(POLICE_NORMALE);
    private static final BaseFont GRASSE = police(POLICE_GRASSE);

    /** Montants alignes a droite, comme dans un tableur (voir Colonnes#estMontant). */
    private static final Pattern NOMBRE = Pattern.compile("-?\\d[\\d\\s]*([.,]\\d+)?");

    /** Largeur relative maximale d'une colonne, pour qu'un long libelle n'ecrase pas les autres. */
    private static final int LARGEUR_MAX = 40;

    private ExportPdf() {
    }

    public static void ecrire(TableauExport tableau, String pied, OutputStream out) throws IOException {
        Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 36);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PiedDePage(pied));
            document.addTitle(tableau.titre());
            document.open();

            Paragraph titre = new Paragraph(tableau.titre(), new Font(GRASSE, 14, Font.NORMAL, TEAL));
            titre.setSpacingAfter(4);
            document.add(titre);
            Font petite = new Font(NORMALE, 9, Font.NORMAL, Color.DARK_GRAY);
            for (String critere : tableau.criteres()) {
                document.add(new Paragraph(critere, petite));
            }
            Paragraph nombre = new Paragraph(tableau.lignes().size() + " ligne(s)", petite);
            nombre.setSpacingAfter(8);
            document.add(nombre);

            int nbColonnes = tableau.entetes().size();
            if (nbColonnes == 0) {
                document.add(new Paragraph("Aucune colonne a exporter.", petite));
                return;
            }
            PdfPTable table = new PdfPTable(nbColonnes);
            table.setWidthPercentage(100);
            table.setWidths(largeurs(tableau));
            table.setHeaderRows(1);

            Font fEntete = new Font(GRASSE, 8);
            for (String entete : tableau.entetes()) {
                PdfPCell cell = new PdfPCell(new Phrase(entete, fEntete));
                cell.setBackgroundColor(GRIS_ENTETE);
                cell.setPadding(4);
                cell.setBorderColor(Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            Font fCellule = new Font(NORMALE, 7.5f);
            boolean[] montant = new boolean[nbColonnes];
            for (int c = 0; c < nbColonnes; c++) {
                montant[c] = Colonnes.estMontant(tableau.entetes().get(c));
            }
            int i = 0;
            for (List<String> ligne : tableau.lignes()) {
                Color fond = (i++ % 2 == 1) ? GRIS_LIGNE : null;
                for (int c = 0; c < nbColonnes; c++) {
                    String v = c < ligne.size() ? ligne.get(c) : "";
                    PdfPCell cell = new PdfPCell(new Phrase(v, fCellule));
                    if (montant[c] && NOMBRE.matcher(v).matches()) {
                        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    }
                    cell.setPadding(3);
                    cell.setBorderColor(Color.LIGHT_GRAY);
                    if (fond != null) {
                        cell.setBackgroundColor(fond);
                    }
                    table.addCell(cell);
                }
            }
            if (tableau.lignes().isEmpty()) {
                PdfPCell vide = new PdfPCell(new Phrase("Aucune ligne a afficher.", fCellule));
                vide.setColspan(nbColonnes);
                vide.setPadding(4);
                table.addCell(vide);
            }
            document.add(table);
        } catch (DocumentException e) {
            throw new IOException("Generation du PDF impossible", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    /** Largeurs relatives : longueur du plus long contenu de chaque colonne, bornee. */
    private static float[] largeurs(TableauExport tableau) {
        int n = tableau.entetes().size();
        float[] largeurs = new float[n];
        for (int c = 0; c < n; c++) {
            largeurs[c] = Math.min(Math.max(tableau.entetes().get(c).length(), 4), LARGEUR_MAX);
        }
        for (List<String> ligne : tableau.lignes()) {
            for (int c = 0; c < n && c < ligne.size(); c++) {
                largeurs[c] = Math.max(largeurs[c], Math.min(ligne.get(c).length(), LARGEUR_MAX));
            }
        }
        return largeurs;
    }

    private static BaseFont police(String chemin) {
        try (InputStream in = ExportPdf.class.getClassLoader().getResourceAsStream(chemin)) {
            if (in == null) {
                throw new IllegalStateException("Police introuvable dans le classpath : " + chemin);
            }
            return BaseFont.createFont(chemin, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final class PiedDePage extends PdfPageEventHelper {
        private final String texte;
        private final Font font = new Font(NORMALE, 7, Font.NORMAL, Color.GRAY);

        PiedDePage(String texte) {
            this.texte = texte;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Rectangle page = document.getPageSize();
            float y = document.bottomMargin() - 18;
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase(new Chunk(texte, font)), document.leftMargin(), y, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase(new Chunk("Page " + writer.getPageNumber(), font)),
                    page.getWidth() - document.rightMargin(), y, 0);
        }
    }
}
