package com.laposte.adminmobile.web;

import com.laposte.adminmobile.service.CompteAppLierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Port of {@code comptelier.xhtml} (backed by {@code CompteLierCtrl#afficherListe}), extended to
 * also cover {@code listecptelie.xhtml} ("Liste comptes lies" - not in the required route list,
 * a plain export-all variant of this same screen with no filter of its own): when no telephone is
 * given, this shows every linked account instead of an empty result, so one page serves both the
 * filtered-by-phone and list-all use cases.
 */
@Controller
public class CompteLieController {

    private final CompteAppLierService compteAppLierService;

    @Autowired
    public CompteLieController(CompteAppLierService compteAppLierService) {
        this.compteAppLierService = compteAppLierService;
    }

    @GetMapping("/comptes/lies")
    public String lies(@RequestParam(required = false) String telephone, Model model) {
        model.addAttribute("telephone", telephone);
        if (telephone != null && !telephone.isBlank()) {
            model.addAttribute("results", compteAppLierService.compteByTelpehone(telephone.trim()));
        } else {
            model.addAttribute("results", compteAppLierService.allcomptelier());
        }
        return "comptes/lies";
    }
}
