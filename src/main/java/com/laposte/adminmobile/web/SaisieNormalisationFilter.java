package com.laposte.adminmobile.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Met en forme les numeros de telephone et de compte de toutes les requetes avant qu'ils
 * n'atteignent les controleurs (voir {@link Saisies}) : un utilisateur qui tape "+228 90 12 34 56"
 * obtient le meme resultat que "90123456", sur tous les ecrans, sans code specifique.
 */
@Component
public class SaisieNormalisationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean concerne = Collections.list(request.getParameterNames()).stream()
                .anyMatch(n -> Saisies.PARAMS_TELEPHONE.contains(n) || Saisies.PARAMS_COMPTE.contains(n));
        chain.doFilter(concerne ? new RequeteNormalisee(request) : request, response);
    }

    private static final class RequeteNormalisee extends HttpServletRequestWrapper {

        private final Map<String, String[]> parametres;

        RequeteNormalisee(HttpServletRequest request) {
            super(request);
            Map<String, String[]> copie = new LinkedHashMap<>();
            request.getParameterMap().forEach((nom, valeurs) -> copie.put(nom, normaliser(nom, valeurs)));
            this.parametres = Collections.unmodifiableMap(copie);
        }

        private static String[] normaliser(String nom, String[] valeurs) {
            if (!Saisies.PARAMS_TELEPHONE.contains(nom) && !Saisies.PARAMS_COMPTE.contains(nom)) {
                return valeurs;
            }
            String[] resultat = new String[valeurs.length];
            for (int i = 0; i < valeurs.length; i++) {
                resultat[i] = Saisies.PARAMS_TELEPHONE.contains(nom)
                        ? Saisies.normaliserTelephone(valeurs[i])
                        : Saisies.normaliserCompte(valeurs[i]);
            }
            return resultat;
        }

        @Override
        public String getParameter(String name) {
            String[] valeurs = parametres.get(name);
            return valeurs == null || valeurs.length == 0 ? null : valeurs[0];
        }

        @Override
        public String[] getParameterValues(String name) {
            return parametres.get(name);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return parametres;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(parametres.keySet());
        }
    }
}
