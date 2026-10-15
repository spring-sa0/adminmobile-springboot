package com.laposte.adminmobile.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditTest {

    @Test
    void uneSaisieNePeutPasFabriquerDeFaussesLignes() {
        String forge = "x\n2026-10-15 09:00:00.000 CONNEXION utilisateur=admin";
        assertThat(Audit.nettoyer(forge)).doesNotContain("\n").startsWith("x 2026-10-15");
    }

    @Test
    void valeursBorneesEtNullesSignalees() {
        assertThat(Audit.nettoyer(null)).isEqualTo("-");
        assertThat(Audit.nettoyer("a".repeat(500))).hasSize(123).endsWith("...");
    }
}
