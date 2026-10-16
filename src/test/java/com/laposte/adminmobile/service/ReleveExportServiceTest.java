package com.laposte.adminmobile.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Les exports de releves partagent des tables de travail : jamais deux a la fois. */
class ReleveExportServiceTest {

    @Test
    void unSecondExportAttendQueLePremierSoitTermine() throws Exception {
        ReleveExportService service = new ReleveExportService(null, 5);
        AtomicInteger enCours = new AtomicInteger();
        AtomicInteger maxSimultanes = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            Future<?>[] exports = new Future<?>[4];
            for (int i = 0; i < exports.length; i++) {
                exports[i] = pool.submit(() -> service.enExclusivite(() -> {
                    maxSimultanes.accumulateAndGet(enCours.incrementAndGet(), Math::max);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    enCours.decrementAndGet();
                    return null;
                }));
            }
            for (Future<?> export : exports) {
                export.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(maxSimultanes.get()).isEqualTo(1);
    }

    @Test
    void attenteTropLongueDonneUnMessageClair() throws Exception {
        ReleveExportService service = new ReleveExportService(null, 0);
        CountDownLatch premierEnCours = new CountDownLatch(1);
        CountDownLatch libererPremier = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<?> premier = pool.submit(() -> service.enExclusivite(() -> {
                premierEnCours.countDown();
                try {
                    libererPremier.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }));
            assertThat(premierEnCours.await(5, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> service.enExclusivite(() -> "second"))
                    .isInstanceOf(ReleveExportService.ExportOccupeException.class)
                    .hasMessageContaining("Un autre export de releve est en cours");

            libererPremier.countDown();
            premier.get(5, TimeUnit.SECONDS);
            assertThat(service.enExclusivite(() -> "apres")).isEqualTo("apres");
        } finally {
            pool.shutdownNow();
        }
    }
}
