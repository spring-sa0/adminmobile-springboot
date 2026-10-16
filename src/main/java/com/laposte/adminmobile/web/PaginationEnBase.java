package com.laposte.adminmobile.web;

import com.laposte.adminmobile.export.FormatExport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.function.Function;

/**
 * Pagination faite par la base pour les recherches volumineuses (toutes les operations d'une
 * periode) : seule la page affichee est lue, au lieu de charger jusqu'a ~100 000 lignes en memoire
 * puis de les decouper ({@link PaginationInterceptor}, qui reste utilise pour les petites listes).
 * <p>
 * Pour un export ({@code ?export=xlsx|pdf}), la requete lit toute la liste, bornee au plafond du
 * format + 1 ligne : {@link ExportInterceptor} peut ainsi refuser une liste trop longue sans
 * qu'elle soit chargee entierement.
 * <p>
 * Usage dans un controleur :
 * <pre>model.addAttribute("results", paginationEnBase.charger(page -> service.recherche(d1, d2, page), model));</pre>
 */
@Component
public class PaginationEnBase {

    private final int taillePage;
    private final int maxLignesExcel;
    private final int maxLignesPdf;

    public PaginationEnBase(@Value("${app.recherche.taille-page:100}") int taillePage,
                            @Value("${app.export.max-lignes-excel:50000}") int maxLignesExcel,
                            @Value("${app.export.max-lignes-pdf:5000}") int maxLignesPdf) {
        this.taillePage = taillePage;
        this.maxLignesExcel = maxLignesExcel;
        this.maxLignesPdf = maxLignesPdf;
    }

    /** Lit la page demandee (ou la liste a exporter), et prepare la barre de pagination. */
    public <T> List<T> charger(Function<Pageable, Page<T>> requete, Model model) {
        HttpServletRequest request = requeteCourante();

        FormatExport export = FormatExport.demande(request);
        if (export != null) {
            int plafond = export == FormatExport.PDF ? maxLignesPdf : maxLignesExcel;
            return requete.apply(PageRequest.of(0, plafond + 1)).getContent();
        }

        int numero = numeroDemande(request);
        Page<T> page = requete.apply(PageRequest.of(numero - 1, taillePage));
        if (page.getContent().isEmpty() && page.getTotalElements() > 0) {
            // Page au-dela de la fin (lien ancien, total qui a diminue) : on affiche la derniere.
            numero = page.getTotalPages();
            page = requete.apply(PageRequest.of(numero - 1, taillePage));
        }
        if (page.getTotalElements() > taillePage) {
            String query = request.getQueryString();
            model.addAttribute("pagination", new Pagination(numero, taillePage, (int) page.getTotalElements(),
                    request.getRequestURI() + (query == null ? "" : "?" + query)));
        }
        return page.getContent();
    }

    private static int numeroDemande(HttpServletRequest request) {
        try {
            return Math.max(1, Integer.parseInt(request.getParameter("page")));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static HttpServletRequest requeteCourante() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }
}
