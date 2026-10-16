package com.laposte.adminmobile.web;

import com.laposte.adminmobile.service.RecuElectroniqueService;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Port of {@code opgateway.xhtml} ("OP GATEWAY", backed by {@code EtatbeanCtrl.etatgw*}). Like
 * the other "etats" screens, the legacy report was Jasper-compiled with no recoverable SQL - see
 * the provenance note on {@link TransactionEtatController}. Approximated the same way: date range
 * + {@code origine} as a stand-in for the legacy "Operateur" dropdown (TMoney / FLooz / Coris Money).
 * Gated to ROLE_ADMINISTRATEUR by {@code SecurityConfig}'s existing {@code /gateway/**} rule.
 */
@Controller
public class GatewayController {

    private final RecuElectroniqueService recuElectroniqueService;
    private final PaginationEnBase paginationEnBase;

    @Autowired
    public GatewayController(RecuElectroniqueService recuElectroniqueService, PaginationEnBase paginationEnBase) {
        this.recuElectroniqueService = recuElectroniqueService;
        this.paginationEnBase = paginationEnBase;
    }

    @GetMapping("/gateway/liste")
    public String liste(@RequestParam(required = false) String date1,
                         @RequestParam(required = false) String date2,
                         @RequestParam(required = false) String operateur,
                         Model model) {
        model.addAttribute("date1", date1);
        model.addAttribute("date2", date2);
        model.addAttribute("operateur", operateur);
        if (WebDateUtils.hasBoth(date1, date2)) {
            Date d1 = WebDateUtils.parse(date1);
            Date d2 = WebDateUtils.parseEnd(date2);
            String erreurPeriode = WebDateUtils.erreurPeriode(d1, d2);
            if (erreurPeriode != null) {
                model.addAttribute("error", erreurPeriode);
            } else {
                model.addAttribute("results", paginationEnBase.charger(page -> recuElectroniqueService.recupListopParPeriodeEtOrigine(d1, d2, operateur, page), model));
            }
        }
        return "gateway/liste";
    }
}
