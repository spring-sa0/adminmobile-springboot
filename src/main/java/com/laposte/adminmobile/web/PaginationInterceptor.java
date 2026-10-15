package com.laposte.adminmobile.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.List;

/**
 * Pagine a l'affichage toute liste exposee sous le nom "results" (convention de tous les ecrans de
 * liste) : seule la page demandee (?page=N) est envoyee au template, accompagnee d'un objet
 * {@link Pagination} pour la barre de navigation (fragments/pagination.html).
 * <p>
 * La pagination se fait apres la requete : c'est la limite de periode de recherche
 * ({@link WebDateUtils#erreurPeriode}) qui borne le volume lu en base. Ici, on evite d'envoyer
 * des dizaines de milliers de lignes au navigateur.
 */
@Component
public class PaginationInterceptor implements HandlerInterceptor {

    static final String RESULTATS = "results";

    private final int taillePage;

    public PaginationInterceptor(@Value("${app.recherche.taille-page:100}") int taillePage) {
        this.taillePage = taillePage;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView mav) {
        // Un export porte sur toute la liste, pas sur la page affichee (voir ExportFilter).
        if (mav == null || !"GET".equals(request.getMethod()) || ExportInterceptor.exportEnCours(request)
                || (mav.getViewName() != null && mav.getViewName().startsWith("redirect:"))
                || !(mav.getModel().get(RESULTATS) instanceof List<?> resultats)
                || resultats.size() <= taillePage) {
            return;
        }
        String query = request.getQueryString();
        Pagination pagination = new Pagination(numeroDemande(request), taillePage, resultats.size(),
                request.getRequestURI() + (query == null ? "" : "?" + query));
        mav.addObject(RESULTATS, new ArrayList<>(resultats.subList(pagination.indexDebut(), pagination.getFin())));
        mav.addObject("pagination", pagination);
    }

    private static int numeroDemande(HttpServletRequest request) {
        try {
            return Integer.parseInt(request.getParameter("page"));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
