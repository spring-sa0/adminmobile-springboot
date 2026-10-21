package com.laposte.adminmobile.web;

import com.laposte.adminmobile.entities.ModifCodesecret;
import com.laposte.adminmobile.entities.Users;
import com.laposte.adminmobile.service.ModifCodesecretService;
import com.laposte.adminmobile.service.UsersService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Portage du formulaire Oracle Forms {@code assist_eposte.fmb} ("ASSISTANCE UTILISATEUR EPOSTE LORS
 * DE LA CREATION DE COMPTE") : recherche d'un compte e-poste par telephone (nom, prenom, e-mail,
 * derniere connexion, date de creation, etat) et, si l'inscription est restee bloquee sans date de
 * creation, renseignement de cette date (DATE_CREATION = SYSDATE).
 * <p>
 * Ecarts volontaires avec le formulaire :
 * <ul>
 *   <li>apres la mise a jour, l'ecran reaffiche les donnees relues en base (le formulaire montrait
 *       les valeurs d'avant, date de creation toujours vide) ;</li>
 *   <li>un compte qui a deja une date de creation est signale comme tel (le formulaire disait
 *       "Aucun utilisateur trouve") et le bouton n'est pas propose ;</li>
 *   <li>l'operation est tracee dans MODIF_CODESECRET (procedure CREATE_MODIF_CODESECRET presente
 *       dans le formulaire mais jamais appelee) et dans audit.log.</li>
 * </ul>
 * Les procedures GEN_PASSWORD et SEND_SMS du formulaire ne sont appelees par aucun bouton : non portees.
 * Reserve aux administrateurs (regle /parametres/** de SecurityConfig).
 */
@Controller
public class AssistanceCreationController {

    static final String VUE = "parametres/assistance-creation";
    private static final String RETOUR = "redirect:/parametres/assistance-creation";
    /** MOTIF fait 25 caracteres. */
    static final String MOTIF = "Assistance creation";

    private final UsersService usersService;
    private final ModifCodesecretService modifCodesecretService;

    public AssistanceCreationController(UsersService usersService, ModifCodesecretService modifCodesecretService) {
        this.usersService = usersService;
        this.modifCodesecretService = modifCodesecretService;
    }

    @GetMapping("/parametres/assistance-creation")
    public String rechercher(@RequestParam(required = false) String telephone, Model model) {
        model.addAttribute("telephone", telephone);
        if (telephone == null || telephone.isBlank()) {
            return VUE;
        }
        List<Users> comptes = usersService.userbyTelephone(telephone.trim());
        model.addAttribute("candidates", comptes);
        if (comptes.isEmpty()) {
            model.addAttribute("error", "Aucun compte e-poste pour le numero " + telephone.trim() + ".");
        } else {
            model.addAttribute("dateCreationManquante", comptes.get(0).getDateCreation() == null);
        }
        return VUE;
    }

    @PostMapping("/parametres/assistance-creation")
    public String renseignerDateCreation(@RequestParam String telephone, Authentication authentication,
                                         RedirectAttributes redirectAttributes) {
        String tel = telephone.trim();
        redirectAttributes.addAttribute("telephone", tel);
        if (usersService.renseignerDateCreation(tel) == 1) {
            ModifCodesecret audit = new ModifCodesecret();
            audit.setTelephone(numero(tel));
            audit.setUtilisateur(authentication != null ? authentication.getName() : null);
            audit.setMotif(MOTIF);
            audit.setDateetheure(new Date());
            if (audit.getTelephone() != null) {
                modifCodesecretService.create(audit);
            }
            redirectAttributes.addFlashAttribute("success", "Date de creation renseignee pour le compte " + tel + ".");
            return RETOUR;
        }
        // Rien n'a ete modifie : compte inexistant, ou date deja presente (double clic, autre agent...).
        List<Users> comptes = usersService.userbyTelephone(tel);
        if (comptes.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Aucun compte e-poste pour le numero " + tel + ".");
        } else {
            Date existante = comptes.get(0).getDateCreation();
            redirectAttributes.addFlashAttribute("error", "La date de creation du compte " + tel + " est deja renseignee"
                    + (existante == null ? "" : " (" + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(existante) + ")")
                    + " : aucune modification.");
        }
        return RETOUR;
    }

    private static BigDecimal numero(String telephone) {
        try {
            return new BigDecimal(telephone);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
