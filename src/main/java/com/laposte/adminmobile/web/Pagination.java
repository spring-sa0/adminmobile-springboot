package com.laposte.adminmobile.web;

import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Etat de pagination d'une liste de resultats, expose aux templates sous le nom "pagination"
 * (voir {@link PaginationInterceptor} et fragments/pagination.html). Les pages sont numerotees a
 * partir de 1.
 */
public final class Pagination {

    /** Nombre de numeros de page affiches de part et d'autre de la page courante. */
    private static final int VOISINAGE = 2;

    private final int page;
    private final int taille;
    private final int total;
    private final String urlCourante;

    Pagination(int page, int taille, int total, String urlCourante) {
        this.taille = taille;
        this.total = total;
        this.page = Math.max(1, Math.min(page, nombrePages(total, taille)));
        this.urlCourante = urlCourante;
    }

    static int nombrePages(int total, int taille) {
        return Math.max(1, (total + taille - 1) / taille);
    }

    public int getPage() {
        return page;
    }

    public int getTotal() {
        return total;
    }

    public int getPages() {
        return nombrePages(total, taille);
    }

    /** Rang (1-based) de la premiere ligne affichee. */
    public int getDebut() {
        return (page - 1) * taille + 1;
    }

    /** Rang de la derniere ligne affichee. */
    public int getFin() {
        return Math.min(page * taille, total);
    }

    int indexDebut() {
        return (page - 1) * taille;
    }

    public boolean isPremiere() {
        return page == 1;
    }

    public boolean isDerniere() {
        return page == getPages();
    }

    /** URL de la page demandee, en conservant tous les filtres de la recherche. */
    public String url(int numero) {
        return UriComponentsBuilder.fromUriString(urlCourante).replaceQueryParam("page", numero).build().toUriString();
    }

    /** Numeros a afficher : 1, ..., voisins de la page courante, ..., derniere (0 = ellipse). */
    public List<Integer> getNumeros() {
        int pages = getPages();
        List<Integer> numeros = new ArrayList<>();
        int de = Math.max(2, page - VOISINAGE);
        int a = Math.min(pages - 1, page + VOISINAGE);
        numeros.add(1);
        if (de > 2) {
            numeros.add(0);
        }
        for (int i = de; i <= a; i++) {
            numeros.add(i);
        }
        if (a < pages - 1) {
            numeros.add(0);
        }
        if (pages > 1) {
            numeros.add(pages);
        }
        return numeros;
    }
}
