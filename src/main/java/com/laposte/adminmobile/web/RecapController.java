package com.laposte.adminmobile.web;

import com.laposte.adminmobile.service.RecapReportService;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Port of the "Eposte &gt; Recap operations / Recap global" screens: {@code recapclient.xhtml},
 * {@code recapcompte.xhtml}, {@code recapglobalprdt.xhtml}, {@code recapglobal.xhtml}. See
 * {@link com.laposte.adminmobile.repository.RecapReportRepository} for provenance.
 */
@Controller
public class RecapController {

    private final RecapReportService recapReportService;

    @Autowired
    public RecapController(RecapReportService recapReportService) {
        this.recapReportService = recapReportService;
    }

    /** Port of recapclient.xhtml ("RECAP DES OPERATIONS PAR CLIENT"). */
    @GetMapping("/eposte/recap/client")
    public String recapClient(@RequestParam(required = false) String date1,
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
                model.addAttribute("results", recapReportService.recapParClient(d1, d2));
            }
        }
        return "eposte/recap-client";
    }

    /** Port of recapcompte.xhtml ("RECAP DES OPERATIONS PAR COMPTE"). */
    @GetMapping("/eposte/recap/compte")
    public String recapCompte(@RequestParam(required = false) String date1,
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
                model.addAttribute("results", recapReportService.recapParCompte(d1, d2));
            }
        }
        return "eposte/recap-compte";
    }

    /** Port of recapglobalprdt.xhtml ("SYNTHESE DES OPERATIONS EPOSTE PAR PRODUIT"). */
    @GetMapping("/eposte/recap-global/produit")
    public String recapGlobalProduit(@RequestParam(required = false) String date1,
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
                model.addAttribute("results", recapReportService.recapParProduit(d1, d2));
            }
        }
        return "eposte/recap-global-produit";
    }

    /** Port of recapglobal.xhtml ("RECAP GLOBAL DES SOUSCRIPTIONS"). */
    @GetMapping("/eposte/recap-global/souscriptions")
    public String recapGlobalSouscriptions(@RequestParam(required = false) String date1,
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
                model.addAttribute("results", recapReportService.recapSouscriptions(d1, d2));
            }
        }
        return "eposte/recap-global-souscriptions";
    }
}
