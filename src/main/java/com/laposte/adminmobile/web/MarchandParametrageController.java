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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Port of {@code paramarchand.xhtml} and {@code desactmarchand.xhtml} (legacy {@code
 * UserCtrl2#afficheruser()}/{@code paramarchand()}/{@code intitulecompte()}/{@code
 * activermarchand()}/{@code desactivermarchand()}). Under the "Marchand" menu (not /parametres/**),
 * so each handler is explicitly gated with {@code @PreAuthorize("hasAuthority('MARCHAND')")} per
 * the task brief - {@code MarchandController} (the read-only Marchand reports) is left untouched;
 * this is a separate controller mapped on distinct sub-paths of /marchand to avoid any collision.
 */
@Controller
public class MarchandParametrageController {

    private static final Logger log = LoggerFactory.getLogger(MarchandParametrageController.class);

    private final UsersService usersService;
    private final ModifCodesecretService modifCodesecretService;

    @Autowired
    public MarchandParametrageController(UsersService usersService, ModifCodesecretService modifCodesecretService) {
        this.usersService = usersService;
        this.modifCodesecretService = modifCodesecretService;
    }

    /** Port of {@code afficheruser()} (search) + {@code intitulecompte()} (compte-name lookup). */
    @PreAuthorize("hasAuthority('MARCHAND')")
    @GetMapping("/marchand/parametrage")
    public String parametrageForm(@RequestParam(required = false) String telephone,
                                   @RequestParam(required = false) String numerocompte,
                                   Model model) {
        model.addAttribute("telephone", telephone);
        model.addAttribute("numerocompte", numerocompte);
        if (telephone != null && !telephone.isBlank()) {
            List<Users> found = usersService.userbyTelephone(telephone.trim());
            model.addAttribute("candidates", found);
            if (found.isEmpty()) {
                model.addAttribute("error", "Ce compte n'existe pas!");
            }
        }
        if (numerocompte != null && !numerocompte.isBlank()) {
            model.addAttribute("intitule", usersService.getIntitule(numerocompte.trim()));
        }
        return "marchand/parametrage";
    }

    /** Port of {@code paramarchand()}: sets NUMERO_COMPTE and IDPROFIL = 2. */
    @PreAuthorize("hasAuthority('MARCHAND')")
    @PostMapping("/marchand/parametrage")
    public String parametrageSubmit(@RequestParam(required = false) String telephone,
                                     @RequestParam(required = false) String numerocompte,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        if (numerocompte == null || numerocompte.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Numero de compte obligatoire");
        } else if (telephone == null || telephone.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Numero de Telephone Obligatoire");
        } else {
            String tel = telephone.trim();
            String compte = numerocompte.trim();
            usersService.paramMarchand(tel, compte);

            ModifCodesecret audit = new ModifCodesecret();
            audit.setTelephone(parseTelephone(tel));
            audit.setUtilisateur(authentication != null ? authentication.getName() : null);
            audit.setMotif("Parametrage Marchand");
            audit.setPrenom(compte);
            audit.setDateetheure(new Date());
            if (audit.getTelephone() != null) {
                modifCodesecretService.create(audit);
            } else {
                log.warn("Telephone non numerique '{}', ligne d'audit MODIF_CODESECRET non creee", tel);
            }

            redirectAttributes.addFlashAttribute("success", "Parametrage effectue avec succes");
            return "redirect:/marchand/parametrage";
        }
        redirectAttributes.addAttribute("telephone", telephone);
        return "redirect:/marchand/parametrage";
    }

    /** Port of {@code afficheruser()} (search), shared by activer/desactiver marchand. */
    @PreAuthorize("hasAuthority('MARCHAND')")
    @GetMapping("/marchand/activer-desactiver")
    public String activationForm(@RequestParam(required = false) String telephone, Model model) {
        model.addAttribute("telephone", telephone);
        if (telephone != null && !telephone.isBlank()) {
            List<Users> found = usersService.userbyTelephone(telephone.trim());
            model.addAttribute("candidates", found);
            if (found.isEmpty()) {
                model.addAttribute("error", "Ce compte n'existe pas!");
            }
        }
        return "marchand/activer-desactiver";
    }

    /** Port of {@code activermarchand()}: IDPROFIL = 2. */
    @PreAuthorize("hasAuthority('MARCHAND')")
    @PostMapping(value = "/marchand/activer-desactiver", params = "activer")
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
            usersService.activerMarchand(tel);
            createAudit(tel, "Activation Marchand", authentication);
            redirectAttributes.addFlashAttribute("success", "Activation effectuee avec succes");
        }
        redirectAttributes.addAttribute("telephone", telephone);
        return "redirect:/marchand/activer-desactiver";
    }

    /** Port of {@code desactivermarchand()}: IDPROFIL = 1. */
    @PreAuthorize("hasAuthority('MARCHAND')")
    @PostMapping(value = "/marchand/activer-desactiver", params = "desactiver")
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
            usersService.desactiverMarchand(tel);
            createAudit(tel, "Desactivation Marchand", authentication);
            redirectAttributes.addFlashAttribute("success", "Desactivation effectuee avec succes");
        }
        redirectAttributes.addAttribute("telephone", telephone);
        return "redirect:/marchand/activer-desactiver";
    }

    private void createAudit(String telephone, String motif, Authentication authentication) {
        ModifCodesecret audit = new ModifCodesecret();
        audit.setTelephone(parseTelephone(telephone));
        audit.setUtilisateur(authentication != null ? authentication.getName() : null);
        audit.setMotif(motif);
        audit.setDateetheure(new Date());
        if (audit.getTelephone() != null) {
            modifCodesecretService.create(audit);
        } else {
            log.warn("Telephone non numerique '{}', ligne d'audit MODIF_CODESECRET non creee", telephone);
        }
    }

    private static BigDecimal parseTelephone(String telephone) {
        try {
            return BigDecimal.valueOf(Double.parseDouble(telephone));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
