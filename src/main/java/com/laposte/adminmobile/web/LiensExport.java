package com.laposte.adminmobile.web;

import com.laposte.adminmobile.export.FormatExport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * URLs d'export de la liste affichee, exposees aux templates sous le nom "liensExport"
 * (fragments/export.html) : la page courante, avec les memes filtres, sans numero de page (on
 * exporte toute la liste) et avec {@code export=xlsx|pdf}.
 */
public final class LiensExport {

    private final String base;

    private LiensExport(String base) {
        this.base = base;
    }

    static LiensExport depuis(HttpServletRequest request) {
        String query = request.getQueryString();
        return new LiensExport(request.getRequestURI() + (query == null ? "" : "?" + query));
    }

    public String getXlsx() {
        return url(FormatExport.XLSX);
    }

    public String getPdf() {
        return url(FormatExport.PDF);
    }

    private String url(FormatExport format) {
        return UriComponentsBuilder.fromUriString(base)
                .replaceQueryParam("page")
                .replaceQueryParam(FormatExport.PARAMETRE, format.extension())
                .build().toUriString();
    }
}
