package com.laposte.adminmobile.web;

import com.laposte.adminmobile.entities.Users;
import com.laposte.adminmobile.service.ProfilService;
import com.laposte.adminmobile.service.RecuElectroniqueService;
import com.laposte.adminmobile.service.UsersService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Port of {@code modifprofil.xhtml} (legacy {@code UserCtrl2#afficheUser()}/{@code
 * modifierprofil()}/{@code affichercom()}). Reachable only under /parametres/**, which {@code
 * SecurityConfig} already restricts to ROLE_ADMINISTRATEUR.
 * <p>
 * The legacy screen's PrimeFaces AJAX ("blur" on the numero-compte field auto-fills the account
 * holder name via {@code affichercom()}) is reproduced here as a plain, no-JS "Verifier compte"
 * button that re-renders the page with the looked-up name - functionally equivalent, without
 * requiring client-side script.
 */
@Controller
public class ProfilController {

    private final UsersService usersService;
    private final ProfilService profilService;
    private final RecuElectroniqueService recuElectroniqueService;

    @Autowired
    public ProfilController(UsersService usersService,
                             ProfilService profilService,
                             RecuElectroniqueService recuElectroniqueService) {
        this.usersService = usersService;
        this.profilService = profilService;
        this.recuElectroniqueService = recuElectroniqueService;
    }

    /** Port of {@code afficheUser()} (search) + {@code affichercom()} (compte-name lookup). */
    @GetMapping("/parametres/modif-profil")
    public String form(@RequestParam(required = false) String telephone,
                        @RequestParam(required = false) String numeroCompte,
                        Model model) {
        model.addAttribute("telephone", telephone);
        model.addAttribute("profils", profilService.allProfil());

        String prefilledCompte = numeroCompte;

        if (telephone != null && !telephone.isBlank()) {
            List<Users> found = usersService.userbyTelephone(telephone.trim());
            if (found.isEmpty()) {
                model.addAttribute("error", "Aucun utilisateur trouve");
            } else {
                Users u = found.get(0);
                model.addAttribute("foundUser", u);
                model.addAttribute("nomprenom",
                        (u.getNom() != null ? u.getNom() : "") + " " + (u.getPrenom() != null ? u.getPrenom() : ""));
                model.addAttribute("selectedProfil", u.getIdprofil() != null ? u.getIdprofil().getIdprofil() : null);
                if (prefilledCompte == null || prefilledCompte.isBlank()) {
                    prefilledCompte = u.getNumeroCompte() != null ? u.getNumeroCompte().toString() : "";
                }
            }
        }
        model.addAttribute("numeroCompte", prefilledCompte);

        if (numeroCompte != null && !numeroCompte.isBlank()) {
            String nomprenomcom = recuElectroniqueService.recupNomprenomcom(numeroCompte.trim());
            if (nomprenomcom == null) {
                model.addAttribute("compteError", "Le compte " + numeroCompte + " n'existe pas");
            } else {
                model.addAttribute("nomprenomcom", nomprenomcom);
            }
        }

        return "parametres/modif-profil";
    }

    /** Port of {@code modifierprofil()}. */
    @PostMapping("/parametres/modif-profil")
    public String submit(@RequestParam String telephone,
                          @RequestParam(required = false) String idprofil,
                          @RequestParam(required = false) String numeroCompte,
                          RedirectAttributes redirectAttributes) {
        if (idprofil == null || idprofil.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Profil obligatoire");
            redirectAttributes.addAttribute("telephone", telephone);
            return "redirect:/parametres/modif-profil";
        }

        int newProfil;
        try {
            // idprofil is rendered from a BigDecimal (Profil#getIdprofil()) and may come back with
            // a decimal point (e.g. "2.00" depending on the NUMBER column's scale) - parse via
            // BigDecimal rather than Integer.parseInt to tolerate that.
            newProfil = new BigDecimal(idprofil.trim()).intValue();
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("error", "Profil invalide");
            redirectAttributes.addAttribute("telephone", telephone);
            return "redirect:/parametres/modif-profil";
        }

        String cpt = numeroCompte == null ? "" : numeroCompte.trim();
        // Merchant profile (IDPROFIL = 2) requires a linked compte, exactly as in modifierprofil().
        if (newProfil == 2 && (cpt.isEmpty() || "0".equals(cpt))) {
            redirectAttributes.addFlashAttribute("error", "Le numero de compte est obligatoire pour ce profil");
            redirectAttributes.addAttribute("telephone", telephone);
            return "redirect:/parametres/modif-profil";
        }
        if (cpt.isEmpty()) {
            cpt = "0";
        }

        usersService.modifProfil(telephone.trim(), newProfil, cpt);
        redirectAttributes.addFlashAttribute("success", "Modification effectuee avec succes");
        return "redirect:/parametres/modif-profil";
    }
}
