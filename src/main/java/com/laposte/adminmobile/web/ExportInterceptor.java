package com.laposte.adminmobile.web;

import com.laposte.adminmobile.export.FormatExport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 * Prepare l'export des listes (voir {@link ExportFilter}) :
 * <ul>
 *   <li>fournit a chaque page les liens "Excel" / "PDF" (modele "liensExport") ;</li>
 *   <li>lors d'un export, refuse une liste trop longue (plafond par format) : la page est alors
 *       affichee normalement, avec un message, au lieu d'un fichier.</li>
 * </ul>
 * Quand l'export est accepte, {@link PaginationInterceptor} ne decoupe pas la liste : le fichier
 * contient toutes les lignes, pas seulement la page affichee.
 */
@Component
public class ExportInterceptor implements HandlerInterceptor {

    /** Attribut de requete pose quand l'export est refuse : la page HTML est renvoyee telle quelle. */
    static final String EXPORT_REFUSE = ExportInterceptor.class.getName() + ".refuse";

    private final int maxLignesExcel;
    private final int maxLignesPdf;

    public ExportInterceptor(@Value("${app.export.max-lignes-excel:50000}") int maxLignesExcel,
                             @Value("${app.export.max-lignes-pdf:5000}") int maxLignesPdf) {
        this.maxLignesExcel = maxLignesExcel;
        this.maxLignesPdf = maxLignesPdf;
    }

    /** Vrai si la requete est un export accepte (la liste doit alors etre complete). */
    static boolean exportEnCours(HttpServletRequest request) {
        return FormatExport.demande(request) != null && request.getAttribute(EXPORT_REFUSE) == null;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView mav) {
        if (mav == null || !"GET".equals(request.getMethod())
                || (mav.getViewName() != null && mav.getViewName().startsWith("redirect:"))) {
            return;
        }
        mav.addObject("liensExport", LiensExport.depuis(request));

        FormatExport format = FormatExport.demande(request);
        if (format != null && mav.getModel().get(PaginationInterceptor.RESULTATS) instanceof List<?> resultats) {
            int max = format == FormatExport.PDF ? maxLignesPdf : maxLignesExcel;
            if (resultats.size() > max) {
                request.setAttribute(EXPORT_REFUSE, Boolean.TRUE);
                mav.addObject("error", "Export " + format.extension().toUpperCase() + " limite a " + max
                        + " lignes (" + resultats.size() + " trouvees) : reduisez la periode ou les criteres de recherche"
                        + (format == FormatExport.PDF ? ", ou exportez en Excel." : "."));
            }
        }
    }
}
