package com.laposte.adminmobile.web;

import com.laposte.adminmobile.service.CashpowerService;
import com.laposte.adminmobile.service.CashpowerService.TypeAnnulation;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

/**
 * Liste des operations annulees sur une periode avec PROCEDURES.P_ANNULER_OPERATION (formulaire
 * Oracle Forms annul_op ou ecran {@code /annulation-op/cashpower}), CashPower ou non.
 * Les annulations EcoCCP (procedure ANNUL_OP_ECOCCP) ont leur propre etat :
 * {@code /etats/transactions/ecoccp-annulees}.
 * <p>
 * Reserve aux administrateurs (regle /annulation-op/** de SecurityConfig). Periode limitee comme
 * toutes les recherches (WebDateUtils), export Excel / PDF et pagination automatiques.
 */
@Controller
public class AnnulationsController {

    private final CashpowerService cashpowerService;

    public AnnulationsController(CashpowerService cashpowerService) {
        this.cashpowerService = cashpowerService;
    }

    @GetMapping("/annulation-op/annulees")
    public String annulees(@RequestParam(required = false) String date1,
                           @RequestParam(required = false) String date2,
                           @RequestParam(required = false) String type,
                           Model model) {
        TypeAnnulation filtre = typeDemande(type);
        model.addAttribute("date1", date1);
        model.addAttribute("date2", date2);
        model.addAttribute("type", filtre.name());
        if (WebDateUtils.hasBoth(date1, date2)) {
            Date d1 = WebDateUtils.parse(date1);
            Date d2 = WebDateUtils.parseEnd(date2);
            String erreurPeriode = WebDateUtils.erreurPeriode(d1, d2);
            if (erreurPeriode != null) {
                model.addAttribute("error", erreurPeriode);
            } else {
                model.addAttribute("results", cashpowerService.annulations(d1, d2, filtre));
            }
        }
        return "annulation-op/annulees";
    }

    private static TypeAnnulation typeDemande(String type) {
        try {
            return type == null || type.isBlank() ? TypeAnnulation.TOUT : TypeAnnulation.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TypeAnnulation.TOUT;
        }
    }
}
