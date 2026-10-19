package com.laposte.adminmobile.web;

import com.laposte.adminmobile.supervision.CatalogueDiagnostics;
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

    public SupervisionController(EtatSanteService etatSanteService) {
        this.etatSanteService = etatSanteService;
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
