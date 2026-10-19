package com.laposte.adminmobile.web;

import com.laposte.adminmobile.export.FormatExport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Limite le nombre de traitements lourds executes en meme temps : recherches sur une periode
 * (parametres date1 et date2) et exports Excel / PDF. Ce sont eux qui chargent des dizaines de
 * milliers de lignes en memoire et occupent les connexions Oracle ; sans limite, une vague
 * d'utilisateurs lancant de gros etats en meme temps sature la memoire et arrete l'application
 * pour tout le monde.
 * <p>
 * Au-dela de la limite, la requete attend qu'une place se libere ; si l'attente depasse le delai,
 * l'utilisateur recoit une page 503 "service momentanement indisponible, reessayez". Les ecrans
 * legers (consultation d'un compte, parametres, reclamations...) ne sont jamais concernes.
 * <p>
 * Place apres Spring Security (seules les requetes authentifiees prennent une place) et avant
 * {@link ExportFilter}, pour que la construction des fichiers d'export soit comprise dans la limite.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class ChargeFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ChargeFilter.class);

    private final Semaphore places;
    private final int maxSimultanes;
    private final long attenteSecondes;
    private final java.util.concurrent.atomic.AtomicLong refus = new java.util.concurrent.atomic.AtomicLong();
    private volatile long dernierRefus;

    public ChargeFilter(@Value("${app.charge.traitements-lourds-simultanes:8}") int maxSimultanes,
                        @Value("${app.charge.attente-secondes:20}") long attenteSecondes) {
        this.maxSimultanes = maxSimultanes;
        this.attenteSecondes = attenteSecondes;
        this.places = new Semaphore(maxSimultanes, true);
    }

    /** Pour la supervision : traitements lourds en cours. */
    public int getEnCours() {
        return maxSimultanes - places.availablePermits();
    }

    public int getMaxSimultanes() {
        return maxSimultanes;
    }

    /** Requetes refusees (503) depuis le demarrage. */
    public long getRefus() {
        return refus.get();
    }

    /** Instant du dernier refus (millisecondes epoch), 0 si aucun. */
    public long getDernierRefus() {
        return dernierRefus;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !estLourd(request);
    }

    static boolean estLourd(HttpServletRequest request) {
        if (!"GET".equals(request.getMethod())) {
            return false;
        }
        return FormatExport.demande(request) != null
                || (!WebDateUtils.isBlank(request.getParameter("date1")) && !WebDateUtils.isBlank(request.getParameter("date2")));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean obtenu;
        try {
            obtenu = places.tryAcquire(attenteSecondes, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            obtenu = false;
        }
        if (!obtenu) {
            refus.incrementAndGet();
            dernierRefus = System.currentTimeMillis();
            log.warn("Surcharge : {} traitements lourds deja en cours, requete refusee apres {} s d'attente : {}",
                    maxSimultanes, attenteSecondes, request.getRequestURI());
            response.setHeader("Retry-After", "30");
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            places.release();
        }
    }
}
