package com.laposte.adminmobile.web;

import com.laposte.adminmobile.service.RecuElectroniqueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Port of {@code annulop.xhtml} and {@code restaureannul.xhtml}, backed by
 * {@code OpCtrl#afficherope/annulerop/afficheresto/restaureop}. The stored-procedure calls
 * themselves are already ported on {@link RecuElectroniqueService#annulopeco} /
 * {@link RecuElectroniqueService#restaurerop} (see that class); this controller just wires the
 * search-then-confirm screens around them. Routes are gated to ROLE_ADMINISTRATEUR by
 * {@code SecurityConfig}'s existing {@code /annulation-op/**} rule.
 */
@Controller
@RequestMapping("/annulation-op")
public class AnnulationOpController {

    private final RecuElectroniqueService recuElectroniqueService;

    @Autowired
    public AnnulationOpController(RecuElectroniqueService recuElectroniqueService) {
        this.recuElectroniqueService = recuElectroniqueService;
    }

    /** Port of OpCtrl#afficherope(): looks up the operation to annul by its numero_operation. */
    @GetMapping("/annuler")
    public String annulerForm(@RequestParam(required = false) String numop, Model model) {
        model.addAttribute("numop", numop);
        if (numop != null && !numop.isBlank()) {
            var results = recuElectroniqueService.recupop(numop.trim());
            model.addAttribute("results", results);
            if (results.isEmpty()) {
                model.addAttribute("error", "Cette transaction n'existe pas! Veuillez verifier le numero op.");
            }
        }
        return "annulation-op/annuler";
    }

    /** Port of OpCtrl#annulerop(): calls the annul_op_ecoccp stored procedure. */
    @PostMapping("/annuler")
    public String annuler(@RequestParam String numop,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        String login = authentication != null ? authentication.getName() : null;
        try {
            recuElectroniqueService.annulopeco(Double.valueOf(numop.trim()), login);
            redirectAttributes.addFlashAttribute("success", "Operation annulee.");
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("error", "Numero d'operation invalide.");
        }
        redirectAttributes.addAttribute("numop", numop);
        return "redirect:/annulation-op/annuler";
    }

    /** Port of OpCtrl#afficheresto(). */
    @GetMapping("/restaurer")
    public String restaurerForm(@RequestParam(required = false) String numop, Model model) {
        model.addAttribute("numop", numop);
        if (numop != null && !numop.isBlank()) {
            var results = recuElectroniqueService.recupop(numop.trim());
            model.addAttribute("results", results);
            if (results.isEmpty()) {
                model.addAttribute("error", "Cette transaction n'existe pas! Veuillez verifier le numero op.");
            }
        }
        return "annulation-op/restaurer";
    }

    /** Port of OpCtrl#restaureop(): calls the restaurer_op_ecoccp stored procedure. */
    @PostMapping("/restaurer")
    public String restaurer(@RequestParam String numop,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        String login = authentication != null ? authentication.getName() : null;
        try {
            recuElectroniqueService.restaurerop(Double.valueOf(numop.trim()), login);
            redirectAttributes.addFlashAttribute("success", "Operation restauree.");
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("error", "Numero d'operation invalide.");
        }
        redirectAttributes.addAttribute("numop", numop);
        return "redirect:/annulation-op/restaurer";
    }
}
