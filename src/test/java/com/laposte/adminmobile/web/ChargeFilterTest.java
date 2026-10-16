package com.laposte.adminmobile.web;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Limite des traitements lourds simultanes : au-dela, 503 au lieu de saturer la memoire. */
class ChargeFilterTest {

    @Test
    void auDelaDeLaLimiteLesRecherchesLourdesSontRefuseesEn503() throws Exception {
        ChargeFilter filtre = new ChargeFilter(1, 0);
        CountDownLatch premiereEnCours = new CountDownLatch(1);
        CountDownLatch libererPremiere = new CountDownLatch(1);
        HttpServlet recherche = new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
                premiereEnCours.countDown();
                try {
                    libererPremiere.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            MockHttpServletResponse reponse1 = new MockHttpServletResponse();
            Future<?> premiere = pool.submit(() -> {
                filtre.doFilter(rechercheSurPeriode(), reponse1, new MockFilterChain(recherche));
                return null;
            });
            assertThat(premiereEnCours.await(5, TimeUnit.SECONDS)).isTrue();

            MockHttpServletResponse reponse2 = new MockHttpServletResponse();
            filtre.doFilter(rechercheSurPeriode(), reponse2, new MockFilterChain(recherche));
            assertThat(reponse2.getStatus()).isEqualTo(503);
            assertThat(reponse2.getHeader("Retry-After")).isEqualTo("30");

            // Un ecran leger n'est jamais concerne, meme quand la limite est atteinte.
            MockHttpServletResponse reponseLegere = new MockHttpServletResponse();
            filtre.doFilter(new MockHttpServletRequest("GET", "/parametres/modif-profil"), reponseLegere, new MockFilterChain());
            assertThat(reponseLegere.getStatus()).isEqualTo(200);

            libererPremiere.countDown();
            premiere.get(5, TimeUnit.SECONDS);
            assertThat(reponse1.getStatus()).isEqualTo(200);

            // La place est liberee : la recherche suivante passe.
            MockHttpServletResponse reponse3 = new MockHttpServletResponse();
            filtre.doFilter(rechercheSurPeriode(), reponse3, new MockFilterChain());
            assertThat(reponse3.getStatus()).isEqualTo(200);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void lesExportsComptentCommeTraitementsLourds() {
        MockHttpServletRequest export = new MockHttpServletRequest("GET", "/releves/liste");
        export.setParameter("export", "pdf");
        assertThat(ChargeFilter.estLourd(export)).isTrue();
        assertThat(ChargeFilter.estLourd(new MockHttpServletRequest("GET", "/releves/liste"))).isFalse();
        assertThat(ChargeFilter.estLourd(new MockHttpServletRequest("POST", "/annulation-op/annuler"))).isFalse();
    }

    private static MockHttpServletRequest rechercheSurPeriode() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/etats/transactions/global");
        req.setParameter("date1", "2026-01-01");
        req.setParameter("date2", "2026-01-31");
        return req;
    }
}
