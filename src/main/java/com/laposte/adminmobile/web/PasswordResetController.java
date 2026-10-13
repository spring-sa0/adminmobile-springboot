package com.laposte.adminmobile.web;

import com.laposte.adminmobile.entities.ModifCodesecret;
import com.laposte.adminmobile.entities.Users;
import com.laposte.adminmobile.service.ModifCodesecretService;
import com.laposte.adminmobile.service.SmsService;
import com.laposte.adminmobile.service.UsersService;
import com.laposte.adminmobile.util.PasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Port of {@code modifpassword.xhtml} (legacy {@code UserCtrl2#afficheruser()}/{@code
 * generpass()}/{@code modifierpass()}). Reachable only under /parametres/**, which {@code
 * SecurityConfig} already restricts to ROLE_ADMINISTRATEUR.
 * <p>
 * IMPORTANT LIMITATION - not a full password reset: this changes {@code USERS_EPOSTE.CODE_SECRET}
 * (via {@link UsersService#reinitPass}), exactly what the legacy screen did. It does NOT change
 * anything the mobile-money customer actually authenticates with on the mobile app, and it has
 * nothing to do with this admin back-office's own login (see {@code LegacyDbAuthenticationProvider}
 * - admin login here authenticates straight against the Oracle account, not this table). This
 * screen is kept for record-keeping / whatever legacy downstream process reads CODE_SECRET, exactly
 * as ported - no attempt is made to change any real credential.
 */
@Controller
public class PasswordResetController {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);

    private final UsersService usersService;
    private final ModifCodesecretService modifCodesecretService;
    private final SmsService smsService;

    @Autowired
    public PasswordResetController(UsersService usersService,
                                    ModifCodesecretService modifCodesecretService,
                                    SmsService smsService) {
        this.usersService = usersService;
        this.modifCodesecretService = modifCodesecretService;
        this.smsService = smsService;
    }

    /** Port of {@code afficheruser()} (search) + {@code generpass()} (generate a candidate password). */
    @GetMapping("/parametres/modif-password")
    public String form(@RequestParam(required = false) String telephone,
                        @RequestParam(required = false) Boolean generate,
                        Model model) {
        model.addAttribute("telephone", telephone);
        if (telephone != null && !telephone.isBlank()) {
            List<Users> found = usersService.userbyTelephone(telephone.trim());
            model.addAttribute("candidates", found);
            if (found.isEmpty()) {
                model.addAttribute("error", "Ce compte n'existe pas!");
            }
        }
        if (Boolean.TRUE.equals(generate)) {
            model.addAttribute("newpass", PasswordGenerator.generatePassword());
        }
        return "parametres/modif-password";
    }

    /**
     * Generation d'un mot de passe candidat en AJAX, sans recharger la page - evite que le
     * "Motif" deja saisi par l'utilisateur soit perdu (c'etait le bug : l'ancien bouton "Generer"
     * declenchait un GET plein-page qui reinitialisait le formulaire "Modifier").
     */
    @GetMapping(value = "/parametres/modif-password/generate", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String generateAjax() {
        return PasswordGenerator.generatePassword();
    }

    /** Port of {@code modifierpass()}. */
    @PostMapping("/parametres/modif-password")
    public String submit(@RequestParam String telephone,
                          @RequestParam(required = false) String newpass,
                          @RequestParam(required = false) String motif,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        if (motif == null || motif.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Motif obligatoire");
        } else if (motif.length() > ModifCodesecret.MOTIF_MAX) {
            redirectAttributes.addFlashAttribute("error", ModifCodesecret.MOTIF_MAX + " caracteres maximum pour le motif");
        } else if (newpass == null || newpass.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Mot de passe obligatoire");
        } else if (usersService.reinitPass(telephone.trim(), newpass) == 0) {
            redirectAttributes.addFlashAttribute("error", "Ce compte n'existe pas!");
        } else {
            String tel = telephone.trim();

            ModifCodesecret audit = new ModifCodesecret();
            audit.setTelephone(parseTelephone(tel));
            audit.setUtilisateur(authentication != null ? authentication.getName() : null);
            audit.setMotif(motif);
            audit.setDateetheure(new Date());
            if (audit.getTelephone() != null) {
                modifCodesecretService.create(audit);
            } else {
                log.warn("Telephone non numerique '{}', ligne d'audit MODIF_CODESECRET non creee", tel);
            }

            try {
                smsService.sendsms(tel, "VOTRE MOT DE PASSE A ETE REINITIALISE. NOUVEAU MOT DE PASSE EST :" + newpass);
            } catch (Exception e) {
                log.warn("Echec de l'envoi du SMS de reinitialisation pour {}", tel, e);
            }

            redirectAttributes.addFlashAttribute("success", "Mot de passe reinitialise avec succes");
            return "redirect:/parametres/modif-password";
        }
        redirectAttributes.addAttribute("telephone", telephone);
        return "redirect:/parametres/modif-password";
    }

    /** {@code MODIF_CODESECRET.TELEPHONE} is a BigDecimal; guard against a non-numeric phone value. */
    private static BigDecimal parseTelephone(String telephone) {
        try {
            return BigDecimal.valueOf(Double.parseDouble(telephone));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
