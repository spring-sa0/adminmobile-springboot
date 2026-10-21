package com.laposte.adminmobile.web;

import com.laposte.adminmobile.service.MailService;
import com.laposte.adminmobile.supervision.CatalogueDiagnostics;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;
import com.laposte.adminmobile.supervision.Diagnostic;
import com.laposte.adminmobile.supervision.EtatSanteService;
import com.laposte.adminmobile.supervision.GroupeErreurs;
import com.laposte.adminmobile.supervision.JournalErreurs;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Console de supervision (administrateurs, regle /supervision/** de SecurityConfig) : etat de
 * sante de l'application, erreurs regroupees par cause avec leur diagnostic et les etapes de
 * resolution, catalogue des causes connues.
 */
@Controller
public class SupervisionController {

    private final EtatSanteService etatSanteService;
    private final MailService mailService;
    private final List<String> destinatairesAlertes;

    public SupervisionController(EtatSanteService etatSanteService, MailService mailService,
                                 @Value("${app.supervision.alertes.destinataires:}") String destinatairesAlertes) {
        this.etatSanteService = etatSanteService;
        this.mailService = mailService;
        this.destinatairesAlertes = Arrays.stream(destinatairesAlertes.split("[,;\\s]+")).filter(s -> !s.isBlank()).toList();
    }

    /**
     * Envoie un e-mail de test aux destinataires des alertes : verifie toute la chaine (serveur,
     * STARTTLS, identifiants, expediteur) et, en cas d'echec, affiche le diagnostic et les etapes de
     * resolution. Un seul essai par clic : pas de tentatives repetees qui verrouilleraient le compte SMTP.
     */
    @PostMapping("/supervision/test-mail")
    public String testerMail(Authentication authentication, RedirectAttributes redirectAttributes) {
        if (destinatairesAlertes.isEmpty()) {
            redirectAttributes.addFlashAttribute("echecMail", CatalogueDiagnostics.diagnostiquer("Illegal address"));
            redirectAttributes.addFlashAttribute("detailEchecMail",
                    "Aucun destinataire : renseigner SUPERVISION_ALERTES_EMAILS dans .env.");
            return "redirect:/supervision";
        }
        String destinataire = destinatairesAlertes.get(0);
        try {
            mailService.sendMessage("[adminmobile] E-mail de test de la supervision",
                    "Test demande par " + (authentication != null ? authentication.getName() : "?")
                            + " depuis la console de supervision.\n\nSi vous lisez ce message, les alertes par e-mail fonctionnent.\n"
                            + "Expediteur : " + mailService.getExpediteur(), destinataire);
            redirectAttributes.addFlashAttribute("success", "E-mail de test envoye a " + destinataire
                    + " (expediteur " + mailService.getExpediteur() + "). Verifiez sa reception.");
        } catch (RuntimeException e) {
            StringBuilder texte = new StringBuilder();
            for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
                texte.append(t.getClass().getName()).append(": ").append(t.getMessage()).append(' ');
            }
            redirectAttributes.addFlashAttribute("echecMail", CatalogueDiagnostics.diagnostiquer(texte.toString()));
            Throwable racine = e;
            while (racine.getCause() != null && racine.getCause() != racine) {
                racine = racine.getCause();
            }
            redirectAttributes.addFlashAttribute("detailEchecMail", "Reponse du serveur : " + racine.getMessage());
        }
        return "redirect:/supervision";
    }

    @GetMapping("/supervision")
    public String tableau(@RequestParam(required = false) String gravite,
                          @RequestParam(defaultValue = "false") boolean resolues,
                          Model model) {
        List<GroupeErreurs> tous = JournalErreurs.instance().groupes();
        Diagnostic.Gravite filtre = gravite(gravite);

        Map<Diagnostic.Gravite, Long> parGravite = new EnumMap<>(Diagnostic.Gravite.class);
        for (Diagnostic.Gravite g : Diagnostic.Gravite.values()) {
            parGravite.put(g, tous.stream().filter(e -> !e.isResolu() && e.getDiagnostic().gravite() == g).count());
        }

        model.addAttribute("etat", etatSanteService.etat());
        model.addAttribute("groupes", tous.stream()
                .filter(g -> resolues || !g.isResolu())
                .filter(g -> filtre == null || g.getDiagnostic().gravite() == filtre)
                .toList());
        model.addAttribute("parGravite", parGravite);
        model.addAttribute("gravites", Diagnostic.Gravite.values());
        model.addAttribute("nbResolues", tous.stream().filter(GroupeErreurs::isResolu).count());
        model.addAttribute("gravite", filtre == null ? "" : filtre.name());
        model.addAttribute("resolues", resolues);
        return "supervision/tableau";
    }

    @GetMapping("/supervision/erreurs/{id}")
    public String erreur(@PathVariable String id, Model model) {
        GroupeErreurs groupe = JournalErreurs.instance().groupe(id);
        if (groupe == null) {
            return "redirect:/supervision";
        }
        model.addAttribute("groupe", groupe);
        model.addAttribute("derniere", groupe.getDerniereOccurrence());
        return "supervision/erreur";
    }

    @PostMapping("/supervision/erreurs/{id}/resolu")
    public String marquerResolu(@PathVariable String id, Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        String par = authentication != null ? authentication.getName() : null;
        if (JournalErreurs.instance().marquerResolu(id, par)) {
            redirectAttributes.addFlashAttribute("success",
                    "Erreur marquee comme resolue. Elle reapparaitra dans la liste si elle se reproduit.");
        }
        return "redirect:/supervision";
    }

    @GetMapping("/supervision/catalogue")
    public String catalogue(Model model) {
        model.addAttribute("regles", CatalogueDiagnostics.regles());
        return "supervision/catalogue";
    }

    private static Diagnostic.Gravite gravite(String valeur) {
        try {
            return valeur == null || valeur.isBlank() ? null : Diagnostic.Gravite.valueOf(valeur.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
