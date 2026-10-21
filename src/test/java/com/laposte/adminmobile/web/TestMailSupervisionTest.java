package com.laposte.adminmobile.web;

import com.laposte.adminmobile.service.MailService;
import com.laposte.adminmobile.supervision.Diagnostic;
import com.laposte.adminmobile.supervision.EtatSanteService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;

/** Bouton "Tester l'envoi d'e-mail" de la supervision. */
class TestMailSupervisionTest {

    private final MailService mail = Mockito.mock(MailService.class);
    private final SupervisionController controleur = new SupervisionController(Mockito.mock(EtatSanteService.class), mail,
            "succes.atchemi@laposte.tg, dsi@laposte.tg");

    /** Cas reel rencontre : STARTTLS accepte, puis identifiants refuses par webmail.laposte.tg. */
    @Test
    void unRefusDuServeurEstAfficheAvecSaSolution() {
        Mockito.doThrow(new MailAuthenticationException(
                        new jakarta.mail.AuthenticationFailedException("535 5.7.8 Error: authentication failed: authentication failure")))
                .when(mail).sendMessage(anyString(), anyString(), anyString());

        RedirectAttributesModelMap attributs = new RedirectAttributesModelMap();
        controleur.testerMail(new UsernamePasswordAuthenticationToken("admin1", null, List.of()), attributs);

        Diagnostic d = (Diagnostic) attributs.getFlashAttributes().get("echecMail");
        assertThat(d.code()).isEqualTo("MAIL_AUTHENTIFICATION");
        assertThat(String.join(" ", d.actions())).contains("MAIL_USERNAME").contains("MAIL_PASSWORD");
        assertThat((String) attributs.getFlashAttributes().get("detailEchecMail")).contains("535 5.7.8");
    }

    @Test
    void unEnvoiReussiEstConfirmeAuPremierDestinataire() {
        Mockito.when(mail.getExpediteur()).thenReturn("sms.contact@laposte.tg");
        RedirectAttributesModelMap attributs = new RedirectAttributesModelMap();
        controleur.testerMail(new UsernamePasswordAuthenticationToken("admin1", null, List.of()), attributs);

        Mockito.verify(mail).sendMessage(contains("E-mail de test"), contains("admin1"), eq("succes.atchemi@laposte.tg"));
        assertThat((String) attributs.getFlashAttributes().get("success")).contains("succes.atchemi@laposte.tg");
        Mockito.verify(mail, Mockito.times(1)).sendMessage(anyString(), anyString(), anyString());
    }
}
