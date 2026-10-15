package com.laposte.adminmobile.export;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ColonnesTest {

    @Test
    void reconnaitLesColonnesDeMontants() {
        assertThat(Colonnes.estMontant("Montant")).isTrue();
        assertThat(Colonnes.estMontant("Solde apres")).isTrue();
        assertThat(Colonnes.estMontant("Com SPT")).isTrue();
        assertThat(Colonnes.estMontant("Total commissions")).isTrue();
        // Identifiants : pas de separateur de milliers ni d'alignement a droite.
        assertThat(Colonnes.estMontant("Compte")).isFalse();
        assertThat(Colonnes.estMontant("Numero Compte")).isFalse();
        assertThat(Colonnes.estMontant("Telephone")).isFalse();
        assertThat(Colonnes.estMontant("Numero operation")).isFalse();
    }
}
