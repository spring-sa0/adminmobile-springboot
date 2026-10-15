package com.laposte.adminmobile.web;

import com.laposte.adminmobile.security.Audit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Trace dans le journal d'audit chaque action (requete POST) : qui, depuis quelle IP, sur quel
 * ecran, avec quels parametres et quel resultat a ete affiche ("success"/"error" du message
 * flash). Couvre ainsi toutes les actions sensibles - annulation / restauration d'operation,
 * changement de profil, de mot de passe, d'e-mail, activation, parametrage marchand, suppression
 * de releve... - sans code dans chaque controleur.
 */
@Component
public class AuditInterceptor implements HandlerInterceptor {

    /** Jamais dans le journal : mots de passe et jeton CSRF. */
    private static final Set<String> MASQUES = Set.of("password", "newpass", "_csrf");

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!"POST".equals(request.getMethod())) {
            return;
        }
        Map<String, String> details = new LinkedHashMap<>();
        details.put("action", request.getMethod() + " " + request.getRequestURI());
        request.getParameterMap().forEach((nom, valeurs) -> {
            if (MASQUES.contains(nom)) {
                details.put(nom, "***");
            } else if (valeurs.length > 0) {
                details.put(nom, String.join(",", valeurs));
            }
        });
        details.put("resultat", resultat(request, response, ex));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Audit.evenement("ACTION", auth != null ? auth.getName() : null, request.getRemoteAddr(), details);
    }

    private static String resultat(HttpServletRequest request, HttpServletResponse response, Exception ex) {
        if (ex != null || response.getStatus() >= 500) {
            return "ERREUR_TECHNIQUE";
        }
        FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
        if (flash.get("error") != null) {
            return "REFUS: " + flash.get("error");
        }
        if (flash.get("success") != null) {
            return "OK: " + flash.get("success");
        }
        if (flash.get("message") != null) {
            return "OK: " + flash.get("message");
        }
        return "HTTP " + response.getStatus();
    }
}
