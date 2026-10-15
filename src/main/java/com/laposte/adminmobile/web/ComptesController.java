package com.laposte.adminmobile.web;

import com.laposte.adminmobile.service.ComptesReportService;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Port of the "Etats &gt; Comptes" screens: {@code nbrecpteco.xhtml}, {@code listecompteco.xhtml},
 * {@code cptactif.xhtml}, {@code soldecompte.xhtml}, {@code listesoldecompte.xhtml},
 * {@code comptenegatif.xhtml}. See {@link com.laposte.adminmobile.repository.ComptesReportRepository}
 * for the provenance caveat shared by every screen in this controller (Jasper-only legacy pages,
 * no recoverable SQL, best-faith reconstruction against SPT.COMPTE / SPT.RECU_ELECTRONIQUE).
 */
@Controller
@RequestMapping("/comptes")
public class ComptesController {

    private final ComptesReportService comptesReportService;

    @Autowired
    public ComptesController(ComptesReportService comptesReportService) {
        this.comptesReportService = comptesReportService;
    }

    /** Port of nbrecpteco.xhtml ("Nombre Compte Eco"): a single count. */
    @GetMapping("/nombre")
    public String nombre(@RequestParam(required = false) String date1,
                          @RequestParam(required = false) String date2,
                          Model model) {
        model.addAttribute("date1", date1);
        model.addAttribute("date2", date2);
        if (WebDateUtils.hasBoth(date1, date2)) {
            Date d1 = WebDateUtils.parse(date1);
            Date d2 = WebDateUtils.parseEnd(date2);
            String erreurPeriode = WebDateUtils.erreurPeriode(d1, d2);
            if (erreurPeriode != null) {
                model.addAttribute("error", erreurPeriode);
            } else {
                model.addAttribute("total", comptesReportService.countComptes(d1, d2));
            }
        }
        return "comptes/nombre";
    }

    /** Port of listecompteco.xhtml ("Liste Compte Eco"). */
    @GetMapping("/liste-ecoccp")
    public String listeEcoccp(@RequestParam(required = false) String date1,
                               @RequestParam(required = false) String date2,
                               Model model) {
        model.addAttribute("date1", date1);
        model.addAttribute("date2", date2);
        if (WebDateUtils.hasBoth(date1, date2)) {
            Date d1 = WebDateUtils.parse(date1);
            Date d2 = WebDateUtils.parseEnd(date2);
            String erreurPeriode = WebDateUtils.erreurPeriode(d1, d2);
            if (erreurPeriode != null) {
                model.addAttribute("error", erreurPeriode);
            } else {
                model.addAttribute("results", comptesReportService.listComptes(d1, d2));
            }
        }
        return "comptes/liste-ecoccp";
    }

    /** Port of cptactif.xhtml ("Nombre Compte Eco Actifs"). */
    @GetMapping("/actifs")
    public String actifs(@RequestParam(required = false) String date1,
                          @RequestParam(required = false) String date2,
                          Model model) {
        model.addAttribute("date1", date1);
        model.addAttribute("date2", date2);
        if (WebDateUtils.hasBoth(date1, date2)) {
            Date d1 = WebDateUtils.parse(date1);
            Date d2 = WebDateUtils.parseEnd(date2);
            String erreurPeriode = WebDateUtils.erreurPeriode(d1, d2);
            if (erreurPeriode != null) {
                model.addAttribute("error", erreurPeriode);
            } else {
                model.addAttribute("results", comptesReportService.listComptesActifs(d1, d2));
            }
        }
        return "comptes/actifs";
    }

    /** Port of soldecompte.xhtml ("Solde Compte Eco"): accounts with movement in the period. */
    @GetMapping("/solde")
    public String solde(@RequestParam(required = false) String date1,
                         @RequestParam(required = false) String date2,
                         Model model) {
        model.addAttribute("date1", date1);
        model.addAttribute("date2", date2);
        if (WebDateUtils.hasBoth(date1, date2)) {
            Date d1 = WebDateUtils.parse(date1);
            Date d2 = WebDateUtils.parseEnd(date2);
            String erreurPeriode = WebDateUtils.erreurPeriode(d1, d2);
            if (erreurPeriode != null) {
                model.addAttribute("error", erreurPeriode);
            } else {
                model.addAttribute("results", comptesReportService.solde(d1, d2));
            }
        }
        return "comptes/solde";
    }

    /** Port of listesoldecompte.xhtml ("Liste Solde Compte Eco"): every account. */
    @GetMapping("/liste-solde")
    public String listeSolde(@RequestParam(required = false) String date1,
                              @RequestParam(required = false) String date2,
                              Model model) {
        model.addAttribute("date1", date1);
        model.addAttribute("date2", date2);
        if (WebDateUtils.hasBoth(date1, date2)) {
            Date d1 = WebDateUtils.parse(date1);
            Date d2 = WebDateUtils.parseEnd(date2);
            String erreurPeriode = WebDateUtils.erreurPeriode(d1, d2);
            if (erreurPeriode != null) {
                model.addAttribute("error", erreurPeriode);
            } else {
                model.addAttribute("results", comptesReportService.listeSolde(d1, d2));
            }
        }
        return "comptes/liste-solde";
    }

    /**
     * Port of comptenegatif.xhtml ("Compte Eco Debiteur"): no date fields on the legacy screen
     * (just a "GENERER" button), so this shows every currently-negative account, unfiltered.
     */
    @GetMapping("/negatifs")
    public String negatifs(Model model) {
        model.addAttribute("results", comptesReportService.comptesNegatifs());
        return "comptes/negatifs";
    }
}
