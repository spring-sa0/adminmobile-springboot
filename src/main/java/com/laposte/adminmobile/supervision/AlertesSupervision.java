package com.laposte.adminmobile.supervision;

import com.laposte.adminmobile.service.MailService;
import com.laposte.adminmobile.supervision.EtatSanteService.EtatSante;
import com.laposte.adminmobile.supervision.EtatSanteService.Indicateur;
import com.laposte.adminmobile.supervision.EtatSanteService.Niveau;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Alertes par e-mail, verifiees chaque minute :
 * <ul>
 *   <li>indicateur de sante CRITIQUE (base injoignable, compte Oracle expire / verrouille, pool ou
 *       memoire sature...), et ALERTE pour l'expiration prochaine du mot de passe et le disque ;</li>
 *   <li>nouvelle erreur de gravite CRITIQUE ou ELEVEE (cause jamais vue depuis le dernier controle).</li>
 * </ul>
 * Une meme alerte n'est pas renvoyee avant {@code app.supervision.alertes.repetition} (1 h) ; un
 * message de retablissement est envoye quand un indicateur revient a la normale. Sans destinataire
 * configure, les alertes sont seulement journalisees.
 */
@Component
public class AlertesSupervision {

    private static final Logger log = LoggerFactory.getLogger(AlertesSupervision.class);
    /** Indicateurs dont le niveau ALERTE merite deja un e-mail (anticipation). */
    private static final Set<String> ALERTE_SUFFIT = Set.of("COMPTE_ORACLE", "DISQUE");

    private final EtatSanteService etatSanteService;
    private final MailService mailService;
    private final List<String> destinataires;
    private final Duration repetition;
    private final String urlSupervision;

    private final Map<String, Long> dernierEnvoi = new HashMap<>();
    private final Set<String> indicateursEnAlerte = new HashSet<>();
    private long dernierControle = System.currentTimeMillis();
    private boolean smtpManquantSignale;

    public AlertesSupervision(EtatSanteService etatSanteService, MailService mailService,
                              @Value("${app.supervision.alertes.destinataires:}") String destinataires,
                              @Value("${app.supervision.alertes.repetition:1h}") Duration repetition,
                              @Value("${app.supervision.url:}") String urlSupervision) {
        this.etatSanteService = etatSanteService;
        this.mailService = mailService;
        this.destinataires = Arrays.stream(destinataires.split("[,;\\s]+")).filter(s -> !s.isBlank()).toList();
        this.repetition = repetition;
        this.urlSupervision = urlSupervision;
    }

    /** Pour l'etat de sante : destinataires configures. */
    public List<String> getDestinataires() {
        return destinataires;
    }

    @Scheduled(initialDelayString = "${app.supervision.alertes.delai-initial:2m}",
            fixedDelayString = "${app.supervision.alertes.intervalle:1m}")
    public synchronized void controler() {
        long maintenant = System.currentTimeMillis();
        List<String> alertes = new ArrayList<>();

        EtatSante etat = etatSanteService.etat();
        for (Indicateur i : etat.indicateurs()) {
            boolean anormal = i.niveau() == Niveau.CRITIQUE
                    || (i.niveau() == Niveau.ALERTE && ALERTE_SUFFIT.contains(i.code()));
            if (anormal) {
                if (aEnvoyer("INDICATEUR:" + i.code(), maintenant)) {
                    alertes.add("[" + i.niveau() + "] " + i.nom() + " : " + i.valeur() + "\n    " + i.detail()
                            + (i.conseil().isBlank() ? "" : "\n    Que faire : " + i.conseil()));
                }
                indicateursEnAlerte.add(i.code());
            } else if (indicateursEnAlerte.remove(i.code())) {
                dernierEnvoi.remove("INDICATEUR:" + i.code());
                alertes.add("[RETABLI] " + i.nom() + " : " + i.valeur() + " - " + i.detail());
            }
        }

        for (GroupeErreurs g : JournalErreurs.instance().groupes()) {
            Diagnostic d = g.getDiagnostic();
            boolean grave = d.gravite() == Diagnostic.Gravite.CRITIQUE || d.gravite() == Diagnostic.Gravite.ELEVEE;
            if (grave && g.getDerniere() > dernierControle && aEnvoyer("ERREUR:" + g.getSignature(), maintenant)) {
                EvenementErreur e = g.getDerniereOccurrence();
                alertes.add("[ERREUR " + d.gravite() + "] " + d.titre() + " (" + g.getNombre() + " occurrence(s))"
                        + "\n    Cause : " + (e == null ? "" : e.causeRacine() + " : " + e.messageCause())
                        + (e != null && e.url() != null ? "\n    Ecran : " + e.url() : "")
                        + (e != null && e.reference() != null ? "\n    Reference : " + e.reference() : "")
                        + "\n    Que faire : " + String.join(" / ", d.actions()));
            }
        }
        dernierControle = maintenant;

        if (!alertes.isEmpty()) {
            envoyer(etat, alertes);
        }
    }

    private boolean aEnvoyer(String cle, long maintenant) {
        Long precedent = dernierEnvoi.get(cle);
        if (precedent != null && maintenant - precedent < repetition.toMillis()) {
            return false;
        }
        dernierEnvoi.put(cle, maintenant);
        return true;
    }

    private void envoyer(EtatSante etat, List<String> alertes) {
        String sujet = "[adminmobile] " + (etat.niveau() == Niveau.OK ? "Information" : "Etat " + etat.niveau())
                + " - " + alertes.size() + " alerte(s)";
        String corps = "Supervision de l'application adminmobile (version " + etat.version() + ") - "
                + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()) + "\n\n"
                + String.join("\n\n", alertes)
                + "\n\nEtat global : " + etat.niveau()
                + (urlSupervision.isBlank() ? "" : "\nDetail : " + urlSupervision)
                + "\n\n-- Message automatique. Une meme alerte n'est pas repetee avant " + repetition.toMinutes() + " min.";
        log.warn("ALERTE SUPERVISION : {}", String.join(" | ", alertes).replace('\n', ' '));
        if (destinataires.isEmpty()) {
            return;
        }
        if (!mailService.estConfigure()) {
            // Sans serveur SMTP, chaque envoi echouerait : un seul message, pas une erreur par alerte.
            if (!smtpManquantSignale) {
                log.warn("Supervision : alertes e-mail non envoyees, aucun serveur SMTP configure (MAIL_HOST vide dans .env). "
                        + "Destinataires prevus : {}", destinataires);
                smtpManquantSignale = true;
            }
            return;
        }
        for (String destinataire : destinataires) {
            try {
                mailService.sendMessage(sujet, corps, destinataire);
            } catch (RuntimeException e) {
                log.warn("Supervision : alerte non envoyee a {}", destinataire, e);
            }
        }
    }
}
