package com.laposte.adminmobile.web;

import com.laposte.adminmobile.entities.ModifCodesecret;
import com.laposte.adminmobile.entities.Users;
import com.laposte.adminmobile.service.ModifCodesecretService;
import com.laposte.adminmobile.service.SmsService;
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
 * Port of {@code modifemail.xhtml} (legacy {@code UserCtrl2#afficheruser()}/{@code
 * modifierEmail()}). Reachable only under /parametres/**, which {@code SecurityConfig} already
 * restricts to ROLE_ADMINISTRATEUR.
 * <p>
 * Deliberate deviation from the legacy code: {@code modifierEmail()} cleared {@code ptelephone}
 * and {@code email} to {@code null} BEFORE calling {@code sdao.sendsms(getPtelephone(), "..." +
 * getEmail())}, so the confirmation SMS was always sent to a null number with "null" in the body -
 * an evident bug. Here the SMS is sent while the values are still available, matching the intent
 * (and the sibling {@code modifierpass()}, which did not have this bug).
 */
@Controller
public class EmailUpdateController {

    private static final Logger log = LoggerFactory.getLogger(EmailUpdateController.class);

    private final UsersService usersService;
    private final ModifCodesecretService modifCodesecretService;
    private final SmsService smsService;

    @Autowired
    public EmailUpdateController(UsersService usersService,
                                  ModifCodesecretService modifCodesecretService,
                                  SmsService smsService) {
        this.usersService = usersService;
        this.modifCodesecretService = modifCodesecretService;
        this.smsService = smsService;
    }

    @GetMapping("/parametres/modif-email")
    public String form(@RequestParam(required = false) String telephone, Model model) {
        model.addAttribute("telephone", telephone);
        if (telephone != null && !telephone.isBlank()) {
            List<Users> found = usersService.userbyTelephone(telephone.trim());
            model.addAttribute("candidates", found);
            if (found.isEmpty()) {
                model.addAttribute("error", "Ce compte n'existe pas!");
            }
        }
        return "parametres/modif-email";
    }

    /** Port of {@code modifierEmail()}. */
    @PostMapping("/parametres/modif-email")
    public String submit(@RequestParam(required = false) String telephone,
                          @RequestParam(required = false) String email,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        if (email == null || email.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Adresse e-mail obligatoire !");
        } else if (telephone == null || telephone.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Telephone obligatoire !");
        } else {
            String tel = telephone.trim();
            usersService.updateEmail(tel, email);

            ModifCodesecret audit = new ModifCodesecret();
            audit.setTelephone(parseTelephone(tel));
            audit.setUtilisateur(authentication != null ? authentication.getName() : null);
            audit.setMotif("Modification email");
            audit.setDateetheure(new Date());
            if (audit.getTelephone() != null) {
                modifCodesecretService.create(audit);
            } else {
                log.warn("Telephone non numerique '{}', ligne d'audit MODIF_CODESECRET non creee", tel);
            }

            try {
                smsService.sendsms(tel, "VOTRE MAIL A ETE REINITIALISE. NOUVEAU MAIL EST :" + email);
            } catch (Exception e) {
                log.warn("Echec de l'envoi du SMS de confirmation pour {}", tel, e);
            }

            redirectAttributes.addFlashAttribute("success", "Modification effectuee avec succes");
            return "redirect:/parametres/modif-email";
        }
        redirectAttributes.addAttribute("telephone", telephone);
        return "redirect:/parametres/modif-email";
    }

    private static BigDecimal parseTelephone(String telephone) {
        try {
            return BigDecimal.valueOf(Double.parseDouble(telephone));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
