package com.laposte.adminmobile.supervision;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.laposte.adminmobile.service.MailService;
import com.laposte.adminmobile.supervision.EtatSanteService.EtatSante;
import com.laposte.adminmobile.supervision.EtatSanteService.Indicateur;
import com.laposte.adminmobile.supervision.EtatSanteService.Niveau;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SupervisionTest {

    // ---- Diagnostic --------------------------------------------------------------------------

    @Test
    void lesCausesConnuesSontReconnues() {
        assertThat(code("ORA-00904: \"RE1_0\".\"MOTIF_ANNULATION\" : identificateur non valide")).isEqualTo("SCHEMA_COLONNE_INEXISTANTE");
        assertThat(code("ORA-28001: the password has expired")).isEqualTo("ORACLE_MOT_DE_PASSE_EXPIRE");
        assertThat(code("ORA-28000: the account is locked")).isEqualTo("ORACLE_COMPTE_VERROUILLE");
        assertThat(code("java.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available, request timed out after 30000ms"))
                .isEqualTo("POOL_SATURE");
        assertThat(code("IO Error: The Network Adapter could not establish the connection")).isEqualTo("ORACLE_INJOIGNABLE");
        assertThat(code("JRFontNotFoundException: Font \"Arial\" is not available to the JVM")).isEqualTo("POLICE_ABSENTE");
        assertThat(code("Duplicate row was found and `ASSERT` was specified")).isEqualTo("IDENTIFIANT_NON_UNIQUE");
        assertThat(code("java.lang.OutOfMemoryError: Java heap space")).isEqualTo("MEMOIRE_SATUREE");
        assertThat(code("PLS-00306: wrong number or types of arguments in call to 'ANNUL_OP_ECOCCP'")).isEqualTo("PROCEDURE_SIGNATURE");
        assertThat(code("ClientAbortException: java.io.IOException: Broken pipe")).isEqualTo("CLIENT_DECONNECTE");
        Diagnostic inconnue = CatalogueDiagnostics.diagnostiquer("IllegalStateException: quelque chose d'inedit");
        assertThat(inconnue.code()).isEqualTo("NON_REPERTORIEE");
        assertThat(inconnue.actions()).isNotEmpty();
    }

    @Test
    void chaqueCauseDonneDesEtapesDeResolution() {
        assertThat(CatalogueDiagnostics.regles()).hasSizeGreaterThan(20)
                .allSatisfy(d -> assertThat(d.actions()).isNotEmpty());
    }

    private static String code(String texte) {
        return CatalogueDiagnostics.diagnostiquer(texte).code();
    }

    // ---- Capture et regroupement -------------------------------------------------------------

    @Test
    void lesErreursDeMemeCauseSontRegroupeesAvecLeurContexte() {
        JournalErreurs journal = new JournalErreurs();
        Logger logger = new LoggerContext().getLogger("com.laposte.adminmobile.web.ErrorReferenceAttributes");

        MDC.put("url", "GET /consultations/operations?ptelephone=90658724");
        MDC.put("utilisateur", "AGENT1");
        MDC.put("reference", "16026740");
        try {
            journal.enregistrer(JournalErreursAppender.convertir(evenement(logger, Level.ERROR, "Erreur 16026740 (HTTP 500)",
                    new IllegalStateException("acces base", new SQLException("ORA-00904: \"RE1_0\".\"MOTIF_ANNULATION\" : identificateur non valide")))));
        } finally {
            MDC.clear();
        }
        // Meme cause, autres valeurs variables (reference, numeros) : meme groupe.
        journal.enregistrer(JournalErreursAppender.convertir(evenement(logger, Level.ERROR, "Erreur 0A1B2C3D (HTTP 500)",
                new IllegalStateException("acces base", new SQLException("ORA-00904: \"RE1_0\".\"MOTIF_ANNULATION\" : identificateur non valide")))));
        // Autre code Oracle : autre groupe.
        journal.enregistrer(JournalErreursAppender.convertir(evenement(logger, Level.ERROR, "Erreur 11111111 (HTTP 500)",
                new IllegalStateException("acces base", new SQLException("ORA-00942: table ou vue inexistante")))));

        List<GroupeErreurs> groupes = journal.groupes();
        assertThat(groupes).hasSize(2);
        GroupeErreurs colonne = groupes.stream()
                .filter(g -> g.getDiagnostic().code().equals("SCHEMA_COLONNE_INEXISTANTE")).findFirst().orElseThrow();
        assertThat(colonne.getNombre()).isEqualTo(2);
        EvenementErreur premier = colonne.getRecentes().get(1);
        assertThat(premier.url()).isEqualTo("GET /consultations/operations?ptelephone=90658724");
        assertThat(premier.utilisateur()).isEqualTo("AGENT1");
        assertThat(premier.reference()).isEqualTo("16026740");
        assertThat(premier.causeRacine()).isEqualTo("java.sql.SQLException");
        assertThat(premier.ligneCode()).startsWith("com.laposte.adminmobile.supervision.SupervisionTest");
    }

    @Test
    void lesDoublonsEtLesAvertissementsSansExceptionNeSontPasRetenus() {
        LoggerContext ctx = new LoggerContext();
        assertThat(JournalErreursAppender.retenir(evenement(ctx.getLogger("org.hibernate.engine.jdbc.spi.SqlExceptionHelper"),
                Level.ERROR, "ORA-00904", null))).isFalse();
        assertThat(JournalErreursAppender.retenir(evenement(ctx.getLogger("com.laposte.adminmobile.web.ChargeFilter"),
                Level.WARN, "Surcharge", null))).isFalse();
        // Echec d'envoi de SMS / e-mail : avertissement avec exception dans le code de l'application.
        assertThat(JournalErreursAppender.retenir(evenement(ctx.getLogger("com.laposte.adminmobile.web.PasswordResetController"),
                Level.WARN, "Echec de l'envoi du SMS", new RuntimeException("SMTP")))).isTrue();
    }

    @Test
    void lHistoriqueEtLesResolutionsSurviventAUnRedemarrage(@TempDir Path dossier) {
        Logger logger = new LoggerContext().getLogger("com.laposte.adminmobile.service.JasperReportService");
        JournalErreurs avant = new JournalErreurs();
        avant.configurer(dossier);
        avant.enregistrer(JournalErreursAppender.convertir(evenement(logger, Level.ERROR, "Export",
                new RuntimeException("Impossible de generer le rapport releve_client1",
                        new RuntimeException("JRFontNotFoundException: Font \"Arial\" is not available to the JVM")))));
        String id = avant.groupes().get(0).getId();
        assertThat(avant.marquerResolu(id, "ADMIN1")).isTrue();

        JournalErreurs apres = new JournalErreurs();
        apres.configurer(dossier);
        assertThat(apres.groupes()).hasSize(1);
        GroupeErreurs g = apres.groupes().get(0);
        assertThat(g.getDiagnostic().code()).isEqualTo("POLICE_ABSENTE");
        assertThat(g.isResolu()).isTrue();
        assertThat(g.getResoluPar()).isEqualTo("ADMIN1");

        // La meme erreur se reproduit : elle reapparait.
        apres.enregistrer(JournalErreursAppender.convertir(evenement(logger, Level.ERROR, "Export",
                new RuntimeException("Impossible de generer le rapport releve_client1",
                        new RuntimeException("JRFontNotFoundException: Font \"Arial\" is not available to the JVM")))));
        assertThat(apres.groupes().get(0).isReapparue()).isTrue();
    }

    private static LoggingEvent evenement(Logger logger, Level niveau, String message, Throwable t) {
        LoggingEvent e = new LoggingEvent(Logger.class.getName(), logger, niveau, message, t, null);
        e.setMDCPropertyMap(MDC.getCopyOfContextMap() == null ? Map.of() : MDC.getCopyOfContextMap());
        return e;
    }

    // ---- Etat de sante -----------------------------------------------------------------------

    @Test
    void uneBaseInjoignableEstSignaleeSansEmpecherLesAutresMesures() throws Exception {
        DataSource ds = Mockito.mock(DataSource.class);
        Mockito.when(ds.getConnection()).thenThrow(new SQLException("ORA-28001: the password has expired"));
        EtatSanteService service = new EtatSanteService(ds, vide(), vide(), vide(), System.getProperty("java.io.tmpdir"));

        EtatSante etat = service.etat();
        assertThat(etat.niveau()).isEqualTo(Niveau.CRITIQUE);
        Indicateur base = etat.indicateurs().stream().filter(i -> i.code().equals("BASE")).findFirst().orElseThrow();
        assertThat(base.niveau()).isEqualTo(Niveau.CRITIQUE);
        assertThat(base.detail()).contains("Mot de passe Oracle du compte technique expire");
        assertThat(base.conseil()).contains("DB_PASSWORD");
        // Les mesures locales restent disponibles.
        Indicateur memoire = etat.indicateurs().stream().filter(i -> i.code().equals("MEMOIRE")).findFirst().orElseThrow();
        assertThat(memoire.niveau()).isNotEqualTo(Niveau.INCONNU);
        assertThat(etat.indicateurs()).hasSize(10);
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> vide() {
        ObjectProvider<T> p = Mockito.mock(ObjectProvider.class);
        Mockito.when(p.getIfAvailable()).thenReturn(null);
        return p;
    }

    // ---- Alertes -----------------------------------------------------------------------------

    @Test
    void uneAlerteCritiqueEstEnvoyeeUneFoisPuisLeRetablissement() {
        EtatSanteService etats = Mockito.mock(EtatSanteService.class);
        MailService mail = Mockito.mock(MailService.class);
        Indicateur critique = new Indicateur("COMPTE_ORACLE", "Compte Oracle de l'application", "EXPIRED(GRACE)",
                Niveau.CRITIQUE, "Mot de passe expire", "Changer DB_PASSWORD");
        Indicateur ok = new Indicateur("COMPTE_ORACLE", "Compte Oracle de l'application", "OPEN", Niveau.OK, "Valable", "");
        Mockito.when(etats.etat()).thenReturn(etat(Niveau.CRITIQUE, critique), etat(Niveau.CRITIQUE, critique), etat(Niveau.OK, ok));

        AlertesSupervision alertes = new AlertesSupervision(etats, mail, "exploit@laposte.tg, dsi@laposte.tg",
                Duration.ofHours(1), "https://adminmobile/supervision");
        alertes.controler();
        verify(mail).sendMessage(contains("CRITIQUE"), contains("Que faire : Changer DB_PASSWORD"), eq("exploit@laposte.tg"));
        verify(mail).sendMessage(anyString(), anyString(), eq("dsi@laposte.tg"));

        alertes.controler(); // toujours critique : pas de nouvel e-mail avant 1 h
        verify(mail, times(2)).sendMessage(anyString(), anyString(), anyString());

        alertes.controler(); // retour a la normale
        verify(mail).sendMessage(anyString(), contains("[RETABLI] Compte Oracle de l'application"), eq("exploit@laposte.tg"));
    }

    @Test
    void sansDestinataireAucunEmail() {
        EtatSanteService etats = Mockito.mock(EtatSanteService.class);
        MailService mail = Mockito.mock(MailService.class);
        Mockito.when(etats.etat()).thenReturn(etat(Niveau.CRITIQUE,
                new Indicateur("BASE", "Base Oracle", "Injoignable", Niveau.CRITIQUE, "", "")));
        new AlertesSupervision(etats, mail, "", Duration.ofHours(1), "").controler();
        verify(mail, never()).sendMessage(anyString(), anyString(), anyString());
    }

    private static EtatSante etat(Niveau niveau, Indicateur... indicateurs) {
        return new EtatSante(niveau, new Date(), "test", new Date(), "1 h", List.of(indicateurs));
    }
}
