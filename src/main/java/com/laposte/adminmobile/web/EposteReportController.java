package com.laposte.adminmobile.web;

import com.laposte.adminmobile.service.EposteFabricatedReportService;
import com.laposte.adminmobile.service.ModifCodesecretService;
import com.laposte.adminmobile.service.UsersService;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Port of the standalone Eposte "Etats" screens not covered by {@link EposteOperationController}
 * (operations) or {@link RecapController} (recap): {@code listechequier.xhtml},
 * {@code etatmodifpwd.xhtml}, {@code etatlistecredit.xhtml}, {@code etatcomptedesa.xhtml} and
 * {@code souscript.xhtml}. Chequiers/achats-credit are best-faith reconstructions against an
 * unverified schema - see {@link com.laposte.adminmobile.repository.EposteFabricatedReportRepository}.
 * modif-pwd/comptes-desactives/souscription are solid, non-approximated queries against the real
 * MODIF_CODESECRET / USERS_EPOSTE tables (already-ported JPA entities).
 */
@Controller
public class EposteReportController {

    private final EposteFabricatedReportService eposteFabricatedReportService;
    private final ModifCodesecretService modifCodesecretService;
    private final UsersService usersService;

    @Autowired
    public EposteReportController(EposteFabricatedReportService eposteFabricatedReportService,
                                   ModifCodesecretService modifCodesecretService,
                                   UsersService usersService) {
        this.eposteFabricatedReportService = eposteFabricatedReportService;
        this.modifCodesecretService = modifCodesecretService;
        this.usersService = usersService;
    }

    /** Port of listechequier.xhtml ("LISTE DES DEMANDES DE CHEQUIERS"). TODO: unverified schema. */
    @GetMapping("/eposte/chequiers")
    public String chequiers(@RequestParam(required = false) String date1,
                             @RequestParam(required = false) String date2,
                             Model model) {
        model.addAttribute("date1", date1);
        model.addAttribute("date2", date2);
        if (WebDateUtils.hasBoth(date1, date2)) {
            Date d1 = WebDateUtils.parse(date1);
            Date d2 = WebDateUtils.parse(date2);
            if (d1 == null || d2 == null) {
                model.addAttribute("error", "Dates invalides.");
            } else {
                model.addAttribute("results", eposteFabricatedReportService.chequiers(d1, d2));
            }
        }
        return "eposte/chequiers";
    }

    /** Port of etatmodifpwd.xhtml ("LISTE DES PASSWORD MODIFIES"). */
    @GetMapping("/eposte/modif-pwd")
    public String modifPwd(@RequestParam(required = false) String date1,
                            @RequestParam(required = false) String date2,
                            Model model) {
        model.addAttribute("date1", date1);
        model.addAttribute("date2", date2);
        if (WebDateUtils.hasBoth(date1, date2)) {
            Date d1 = WebDateUtils.parse(date1);
            Date d2 = WebDateUtils.parse(date2);
            if (d1 == null || d2 == null) {
                model.addAttribute("error", "Dates invalides.");
            } else {
                model.addAttribute("results", modifCodesecretService.findByPeriode(d1, d2));
            }
        }
        return "eposte/modif-pwd";
    }

    /** Port of etatlistecredit.xhtml ("LISTE D'ACHAT DE CREDIT MOOV"). TODO: unverified schema. */
    @GetMapping("/eposte/achats-credit")
    public String achatsCredit(@RequestParam(required = false) String date1,
                                @RequestParam(required = false) String date2,
                                Model model) {
        model.addAttribute("date1", date1);
        model.addAttribute("date2", date2);
        if (WebDateUtils.hasBoth(date1, date2)) {
            Date d1 = WebDateUtils.parse(date1);
            Date d2 = WebDateUtils.parse(date2);
            if (d1 == null || d2 == null) {
                model.addAttribute("error", "Dates invalides.");
            } else {
                model.addAttribute("results", eposteFabricatedReportService.achatsCredit(d1, d2));
            }
        }
        return "eposte/achats-credit";
    }

    /**
     * Port of etatcomptedesa.xhtml ("COMPTES EPOSTE DESACTIVES"): no date fields on the legacy
     * screen (just a "Export pdf" button), so this always shows every currently-deactivated account.
     */
    @GetMapping("/eposte/comptes-desactives")
    public String comptesDesactives(Model model) {
        model.addAttribute("results", usersService.comptesDesactives());
        return "eposte/comptes-desactives";
    }

    /** Port of souscript.xhtml ("ETAT DE SOUSCRIPTION DE EPOSTE"). */
    @GetMapping("/eposte/souscription")
    public String souscription(@RequestParam(required = false) String date1,
                                @RequestParam(required = false) String date2,
                                @RequestParam(required = false) String operateur,
                                Model model) {
        model.addAttribute("date1", date1);
        model.addAttribute("date2", date2);
        model.addAttribute("operateur", operateur);
        if (WebDateUtils.hasBoth(date1, date2)) {
            Date d1 = WebDateUtils.parse(date1);
            Date d2 = WebDateUtils.parse(date2);
            if (d1 == null || d2 == null) {
                model.addAttribute("error", "Dates invalides.");
            } else {
                model.addAttribute("results", usersService.souscriptions(d1, d2, operateur));
            }
        }
        return "eposte/souscription";
    }
}
