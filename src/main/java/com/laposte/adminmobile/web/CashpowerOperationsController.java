package com.laposte.adminmobile.web;

import com.laposte.adminmobile.service.CashpowerOperationsService;
import com.laposte.adminmobile.service.CashpowerOperationsService.AchatCashpower;
import com.laposte.adminmobile.service.CashpowerOperationsService.Application;
import com.laposte.adminmobile.service.CashpowerOperationsService.Statut;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Achats CashPower faits depuis les applications (E-Poste, Marchand, autres), classes par statut :
 * equivalent des menus Oracle Forms "Cash Power Eposte" et "Cash Power EPOSTE MARCHAND >
 * Operations Aboutis / Non Aboutis". Voir {@link CashpowerOperationsService}.
 * <p>
 * Regle /etats/** de SecurityConfig (PROFIL_USER). Periode limitee comme toutes les recherches
 * (WebDateUtils), export Excel / PDF et pagination automatiques.
 */
@Controller
public class CashpowerOperationsController {

    private final CashpowerOperationsService service;

    public CashpowerOperationsController(CashpowerOperationsService service) {
        this.service = service;
    }

    @GetMapping("/etats/cashpower")
    public String operations(@RequestParam(required = false) String date1,
                             @RequestParam(required = false) String date2,
                             @RequestParam(required = false) String application,
                             @RequestParam(required = false) String statut,
                             Model model) {
        Application app = valeur(Application.class, application, Application.TOUTES);
        Statut st = valeur(Statut.class, statut, Statut.TOUS);
        model.addAttribute("date1", date1);
        model.addAttribute("date2", date2);
        model.addAttribute("application", app.name());
        model.addAttribute("statut", st.name());
        if (WebDateUtils.hasBoth(date1, date2)) {
            Date d1 = WebDateUtils.parse(date1);
            Date d2 = WebDateUtils.parseEnd(date2);
            String erreurPeriode = WebDateUtils.erreurPeriode(d1, d2);
            if (erreurPeriode != null) {
                model.addAttribute("error", erreurPeriode);
            } else {
                List<AchatCashpower> achats = service.operations(d1, d2, app, st);
                model.addAttribute("recap", CashpowerOperationsService.recapitulatif(achats));
                model.addAttribute("results", achats);
            }
        }
        return "etats/cashpower";
    }

    private static <E extends Enum<E>> E valeur(Class<E> type, String texte, E defaut) {
        try {
            return texte == null || texte.isBlank() ? defaut : Enum.valueOf(type, texte.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return defaut;
        }
    }
}
