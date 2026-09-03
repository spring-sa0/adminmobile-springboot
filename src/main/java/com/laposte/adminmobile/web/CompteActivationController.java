package com.laposte.adminmobile.web;

import com.laposte.adminmobile.entities.ModifCodesecret;
import com.laposte.adminmobile.entities.Users;
import com.laposte.adminmobile.service.ModifCodesecretService;
import com.laposte.adminmobile.service.UsersService;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Port of {@code desactivcompte.xhtml} (legacy {@code UserCtrl2#afficheruser()}/{@code
 * desactiver()}/{@code activer()}). Reachable only under /parametres/**, which {@code
 * SecurityConfig} already restricts to ROLE_ADMINISTRATEUR. Uses {@code
 * UsersService#desactivecompte}/{@code activecompte} (ACTIF = 2 / 0 on USERS_EPOSTE), already
 * present on the shared {@link UsersService}.
 */
@Controller
public class CompteActivationController {

    private static final Logger log = LoggerFactory.getLogger(CompteActivationController.class);

    private final UsersService usersService;
    private final ModifCodesecretService modifCodesecretService;

    @Autowired
    public CompteActivationController(UsersService usersService, ModifCodesecretService modifCodesecretService) {
        this.usersService = usersService;
        this.modifCodesecretService = modifCodesecretService;
    }

    @GetMapping("/parametres/activer-desactiver")
    public String form(@RequestParam(required = false) String telephone, Model model) {
        model.addAttribute("telephone", telephone);
        if (telephone != null && !telephone.isBlank()) {
            List<Users> found = usersService.userbyTelephone(telephone.trim());
            model.addAttribute("candidates", found);
            if (found.isEmpty()) {
                model.addAttribute("error", "Ce compte n'existe pas!");
            }
        }
        return "parametres/activer-desactiver";
    }

    /** Port of {@code desactiver()}: ACTIF = 2, audit row records actif=2 (as the legacy code did). */
    @PostMapping(value = "/parametres/activer-desactiver", params = "desactiver")
    public String desactiver(@RequestParam String telephone,
                              @RequestParam(required = false) String motif,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        if (telephone.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Telephone obligatoire!");
        } else if (motif == null || motif.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Motif obligatoire");
        } else {
            String tel = telephone.trim();
            usersService.desactivecompte(tel);
            ModifCodesecret audit = newAudit(tel, motif, authentication);
            audit.setActif(2);
            if (audit.getTelephone() != null) {
                modifCodesecretService.create(audit);
            } else {
                log.warn("Telephone non numerique '{}', ligne d'audit MODIF_CODESECRET non creee", tel);
            }
            redirectAttributes.addFlashAttribute("success", "Compte desactive avec succes!");
        }
        redirectAttributes.addAttribute("telephone", telephone);
        return "redirect:/parametres/activer-desactiver";
    }

    /** Port of {@code activer()}: ACTIF = 0. */
    @PostMapping(value = "/parametres/activer-desactiver", params = "activer")
    public String activer(@RequestParam String telephone,
                           @RequestParam(required = false) String motif,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        if (telephone.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Telephone obligatoire!");
        } else if (motif == null || motif.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Motif obligatoire");
        } else {
            String tel = telephone.trim();
            usersService.activecompte(tel);
            ModifCodesecret audit = newAudit(tel, motif, authentication);
            if (audit.getTelephone() != null) {
                modifCodesecretService.create(audit);
            } else {
                log.warn("Telephone non numerique '{}', ligne d'audit MODIF_CODESECRET non creee", tel);
            }
            redirectAttributes.addFlashAttribute("success", "Compte active avec succes!");
        }
        redirectAttributes.addAttribute("telephone", telephone);
        return "redirect:/parametres/activer-desactiver";
    }

    private static ModifCodesecret newAudit(String telephone, String motif, Authentication authentication) {
        ModifCodesecret audit = new ModifCodesecret();
        audit.setTelephone(parseTelephone(telephone));
        audit.setUtilisateur(authentication != null ? authentication.getName() : null);
        audit.setMotif(motif);
        audit.setDateetheure(new Date());
        return audit;
    }

    private static BigDecimal parseTelephone(String telephone) {
        try {
            return BigDecimal.valueOf(Double.parseDouble(telephone));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
