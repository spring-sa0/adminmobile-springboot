package com.laposte.adminmobile.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Regles communes a tous les ecrans : duree maximale de la periode de recherche, pagination de
 * l'affichage, validation des telephones / numeros de compte, journal d'audit des actions.
 */
@Configuration
public class RechercheConfig implements WebMvcConfigurer {

    private final PaginationInterceptor paginationInterceptor;
    private final SaisieValidationInterceptor saisieValidationInterceptor;
    private final AuditInterceptor auditInterceptor;
    private final ExportInterceptor exportInterceptor;

    public RechercheConfig(PaginationInterceptor paginationInterceptor,
                           SaisieValidationInterceptor saisieValidationInterceptor,
                           AuditInterceptor auditInterceptor,
                           ExportInterceptor exportInterceptor,
                           @Value("${app.recherche.periode-max-jours:92}") int periodeMaxJours) {
        this.paginationInterceptor = paginationInterceptor;
        this.saisieValidationInterceptor = saisieValidationInterceptor;
        this.auditInterceptor = auditInterceptor;
        this.exportInterceptor = exportInterceptor;
        WebDateUtils.periodeMaxJours = periodeMaxJours;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditInterceptor);
        registry.addInterceptor(saisieValidationInterceptor);
        registry.addInterceptor(paginationInterceptor);
        // Enregistre apres la pagination : les postHandle s'executent en ordre inverse, l'export
        // decide donc (refus eventuel) avant que la pagination ne s'applique.
        registry.addInterceptor(exportInterceptor);
    }
}
