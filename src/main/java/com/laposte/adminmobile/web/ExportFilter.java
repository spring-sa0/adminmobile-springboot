package com.laposte.adminmobile.web;

import com.laposte.adminmobile.export.ExportExcel;
import com.laposte.adminmobile.export.ExportPdf;
import com.laposte.adminmobile.export.ExtracteurTableau;
import com.laposte.adminmobile.export.FormatExport;
import com.laposte.adminmobile.export.TableauExport;
import com.laposte.adminmobile.security.Audit;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Export Excel / PDF de n'importe quelle liste : la page est rendue normalement (toute la liste,
 * sans pagination - voir {@link ExportInterceptor}), puis son tableau est converti en fichier.
 * Le fichier reprend donc exactement les colonnes et le formatage de l'ecran, pour tous les
 * ecrans, sans code d'export propre a chacun.
 * <p>
 * Place apres Spring Security (ordre par defaut des filtres applicatifs) : les regles d'acces de
 * la page s'appliquent a son export. Si la page ne contient pas de liste, renvoie une erreur ou si
 * l'export a ete refuse (liste trop longue), la reponse HTML est transmise telle quelle.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE) // apres ChargeFilter : la construction du fichier compte dans la limite
public class ExportFilter extends OncePerRequestFilter {

    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter SUFFIXE_FICHIER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return FormatExport.demande(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingResponseWrapper page = new ContentCachingResponseWrapper(response);
        chain.doFilter(request, page);

        TableauExport tableau = null;
        if (page.getStatus() == HttpServletResponse.SC_OK && request.getAttribute(ExportInterceptor.EXPORT_REFUSE) == null
                && page.getContentType() != null && page.getContentType().startsWith("text/html")) {
            tableau = ExtracteurTableau.extraire(new String(page.getContentAsByteArray(), charset(page)));
        }
        if (tableau == null) {
            page.copyBodyToResponse();
            return;
        }

        FormatExport format = FormatExport.demande(request);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String utilisateur = auth != null ? auth.getName() : "-";
        LocalDateTime maintenant = LocalDateTime.now();
        String pied = "Exporte le " + HORODATAGE.format(maintenant) + " par " + utilisateur + " - La Poste, administration EcoCCP / E-Poste";

        ByteArrayOutputStream fichier = new ByteArrayOutputStream();
        if (format == FormatExport.PDF) {
            ExportPdf.ecrire(tableau, pied, fichier);
        } else {
            ExportExcel.ecrire(tableau, pied, fichier);
        }

        Audit.evenement("EXPORT", utilisateur, request.getRemoteAddr(), Map.of(
                "format", format.extension(),
                "page", request.getRequestURI() + (request.getQueryString() == null ? "" : "?" + request.getQueryString()),
                "lignes", String.valueOf(tableau.lignes().size())));

        // On remplace la page HTML mise en tampon par le fichier.
        page.resetBuffer();
        // Le charset UTF-8 herite de la page HTML n'a pas de sens pour un fichier binaire.
        response.setCharacterEncoding(null);
        response.setContentType(format.contentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(nomFichier(tableau.titre(), maintenant, format), StandardCharsets.UTF_8).build().toString());
        response.setContentLength(fichier.size());
        fichier.writeTo(response.getOutputStream());
    }

    private static Charset charset(HttpServletResponse response) {
        try {
            return Charset.forName(response.getCharacterEncoding());
        } catch (RuntimeException e) {
            return StandardCharsets.UTF_8;
        }
    }

    /** "Liste des Operations EcoCCP" -> "liste-des-operations-ecoccp-20261015-1430.xlsx". */
    static String nomFichier(String titre, LocalDateTime date, FormatExport format) {
        String base = Normalizer.normalize(titre, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isEmpty()) {
            base = "export";
        }
        if (base.length() > 60) {
            base = base.substring(0, 60).replaceAll("-$", "");
        }
        return base + "-" + SUFFIXE_FICHIER.format(date) + "." + format.extension();
    }
}
