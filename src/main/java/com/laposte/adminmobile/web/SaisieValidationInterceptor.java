package com.laposte.adminmobile.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Refuse les recherches (GET) dont le telephone ou le numero de compte est invalide, apres mise en
 * forme par {@link SaisieNormalisationFilter} : la page est reaffichee avec un message clair au
 * lieu de lancer une requete vouee a ne rien trouver. Les actions POST ne sont pas concernees :
 * elles echouent deja proprement sur un compte inexistant, et les formulaires reclamation /
 * suggestion ont leur propre validation.
 */
@Component
public class SaisieValidationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!"GET".equals(request.getMethod()) || !(handler instanceof HandlerMethod method) || renvoieDesDonnees(method)) {
            return true;
        }
        String erreur = null;
        UriComponentsBuilder retour = ServletUriComponentsBuilder.fromRequest(request);
        for (String nom : Saisies.PARAMS_TELEPHONE) {
            if (!Saisies.telephoneValide(request.getParameter(nom))) {
                erreur = Saisies.MESSAGE_TELEPHONE;
                retour.replaceQueryParam(nom);
            }
        }
        for (String nom : Saisies.PARAMS_COMPTE) {
            if (!Saisies.compteValide(request.getParameter(nom))) {
                erreur = erreur == null ? Saisies.MESSAGE_COMPTE : erreur;
                retour.replaceQueryParam(nom);
            }
        }
        if (erreur == null) {
            return true;
        }
        FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
        flash.put("error", erreur);
        RequestContextUtils.saveOutputFlashMap(request.getRequestURI(), request, response);
        response.sendRedirect(retour.build().toUriString());
        return false;
    }

    /** Les appels AJAX (@ResponseBody, ex. /releves/intitule) ne doivent pas recevoir de redirection. */
    private static boolean renvoieDesDonnees(HandlerMethod method) {
        return method.hasMethodAnnotation(ResponseBody.class)
                || AnnotatedElementUtils.hasAnnotation(method.getBeanType(), ResponseBody.class);
    }
}
