package com.laposte.adminmobile.web;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class SaisiesEtPaginationTest {

    @Test
    void telephoneEstRameneA8Chiffres() {
        assertThat(Saisies.normaliserTelephone("90123456")).isEqualTo("90123456");
        assertThat(Saisies.normaliserTelephone(" 90 12 34 56 ")).isEqualTo("90123456");
        assertThat(Saisies.normaliserTelephone("+228 90.12.34.56")).isEqualTo("90123456");
        assertThat(Saisies.normaliserTelephone("0022890123456")).isEqualTo("90123456");
        assertThat(Saisies.normaliserTelephone("22890123456")).isEqualTo("90123456");
        // Un numero qui ne se ramene pas a 8 chiffres reste tel quel : la validation le refuse.
        assertThat(Saisies.normaliserTelephone("228901")).isEqualTo("228901");
        assertThat(Saisies.telephoneValide("228901")).isFalse();
        assertThat(Saisies.telephoneValide("9012345a")).isFalse();
        assertThat(Saisies.telephoneValide("")).isTrue();
    }

    @Test
    void compteNeContientQueDesChiffres() {
        assertThat(Saisies.normaliserCompte(" 6310 3197 9400 1000 ")).isEqualTo("6310319794001000");
        assertThat(Saisies.compteValide("6310319794001000")).isTrue();
        assertThat(Saisies.compteValide("63103197940010001")).isFalse();
        assertThat(Saisies.compteValide("12A")).isFalse();
    }

    @Test
    void periodeBornee() {
        Date debut = WebDateUtils.parse("2026-01-01");
        assertThat(WebDateUtils.erreurPeriode(debut, WebDateUtils.parseEnd("2026-04-02"))).isNull();
        assertThat(WebDateUtils.erreurPeriode(debut, WebDateUtils.parseEnd("2026-04-03"))).contains("92 jours");
        assertThat(WebDateUtils.erreurPeriode(debut, WebDateUtils.parseEnd("2026-01-01"))).isNull();
        assertThat(WebDateUtils.erreurPeriode(null, debut)).isEqualTo("Dates invalides.");
    }

    @Test
    void numerosDePageAvecEllipses() {
        Pagination p = new Pagination(10, 100, 3_201, "/etats/transactions/global?date1=2026-01-01&page=1");
        assertThat(p.getPages()).isEqualTo(33);
        assertThat(p.getNumeros()).containsExactly(1, 0, 8, 9, 10, 11, 12, 0, 33);
        assertThat(p.getDebut()).isEqualTo(901);
        assertThat(p.getFin()).isEqualTo(1_000);
        assertThat(p.url(11)).isEqualTo("/etats/transactions/global?date1=2026-01-01&page=11");

        Pagination horsBornes = new Pagination(99, 100, 150, "/x");
        assertThat(horsBornes.getPage()).isEqualTo(2);
        assertThat(horsBornes.getNumeros()).containsExactly(1, 2);
    }
}
