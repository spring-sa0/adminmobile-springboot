package com.laposte.adminmobile.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Ajoute au contexte de journalisation (MDC) l'URL et l'utilisateur de la requete en cours : toute
 * erreur journalisee pendant la requete est ainsi rattachee a l'ecran et a l'agent concernes dans
 * la supervision. Place apres Spring Security, pour connaitre l'utilisateur.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 20)
public class ContexteJournalFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String query = request.getQueryString();
        MDC.put("url", request.getMethod() + " " + request.getRequestURI() + (query == null ? "" : "?" + query));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            MDC.put("utilisateur", auth.getName());
        }
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("url");
            MDC.remove("utilisateur");
        }
    }
}
