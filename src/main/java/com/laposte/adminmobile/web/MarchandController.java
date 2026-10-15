package com.laposte.adminmobile.web;

import com.laposte.adminmobile.service.FormatsService;
import com.laposte.adminmobile.service.MarchandReportService;
import com.laposte.adminmobile.service.RecuElectroniqueService;
import com.laposte.adminmobile.service.UsersService;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Port of the "Marchand" (merchant) screens: {@code listecom.xhtml}, {@code opmarchand.xhtml},
 * {@code listeopcom.xhtml}, {@code listeopparcom.xhtml}, {@code listecommicom.xhtml}. A "merchant"
 * account is confirmed to be an {@code EPOSTE.USERS_EPOSTE} row with {@code IDPROFIL = 2} - see
 * {@code UsersRepository#findMerchantAccounts()}. See
 * {@link com.laposte.adminmobile.repository.MarchandReportRepository} for the rest of the
 * provenance notes.
 */
@Controller
@RequestMapping("/marchand")
public class MarchandController {

    private final MarchandReportService marchandReportService;
    private final UsersService usersService;
    private final RecuElectroniqueService recuElectroniqueService;
    private final FormatsService formatsService;

    @Autowired
    public MarchandController(MarchandReportService marchandReportService,
                               UsersService usersService,
                               RecuElectroniqueService recuElectroniqueService,
                               FormatsService formatsService) {
        this.marchandReportService = marchandReportService;
        this.usersService = usersService;
        this.recuElectroniqueService = recuElectroniqueService;
        this.formatsService = formatsService;
    }

    /** Port of listecom.xhtml ("Liste comptes Marchands"): no filters on the legacy screen. */
    @GetMapping("/comptes")
    public String comptes(Model model) {
        model.addAttribute("results", usersService.merchantAccounts());
        return "marchand/comptes";
    }

    /**
     * Port of opmarchand.xhtml ("Liste des operations marchands"). {@code listeopcom.xhtml}
     * ("Liste des operations des marchands", below) is - per its own title and its
     * {@code EtatbeanCtrl} wiring ({@code etatopcompdf}/{@code listopcomxls}, distinct methods but
     * with identical date-only filters and no other clue distinguishing the two reports) - a
     * near-duplicate of this same screen in the legacy source; both are wired to the same query here.
     */
    @GetMapping("/operations")
    public String operations(@RequestParam(required = false) String date1,
                              @RequestParam(required = false) String date2,
                              Model model) {
        return renderOperations(date1, date2, model, "marchand/operations");
    }

    /** Port of listeopcom.xhtml - see {@link #operations} javadoc for why it reuses the same query. */
    @GetMapping("/operations-liste")
    public String operationsListe(@RequestParam(required = false) String date1,
                                   @RequestParam(required = false) String date2,
                                   Model model) {
        return renderOperations(date1, date2, model, "marchand/operations-liste");
    }

    /** Port of listeopparcom.xhtml ("Liste des operations par marchands"): filtered by numero compte. */
    @GetMapping("/operations-par-marchand")
    public String operationsParMarchand(@RequestParam(required = false) String date1,
                                         @RequestParam(required = false) String date2,
                                         @RequestParam(required = false) String numerocompte,
                                         Model model) {
        model.addAttribute("date1", date1);
        model.addAttribute("date2", date2);
        model.addAttribute("numerocompte", numerocompte);
        if (!WebDateUtils.isBlank(numerocompte)) {
            model.addAttribute("intitule", formatsService.getIntitule(numerocompte.trim()));
            if (!WebDateUtils.hasBoth(date1, date2)) {
                model.addAttribute("error", "Veuillez renseigner les deux dates.");
            } else {
                Date d1 = WebDateUtils.parse(date1);
                Date d2 = WebDateUtils.parseEnd(date2);
                String erreurPeriode = WebDateUtils.erreurPeriode(d1, d2);
                if (erreurPeriode != null) {
                    model.addAttribute("error", erreurPeriode);
                } else {
                    try {
                        model.addAttribute("results",
                                recuElectroniqueService.recupListopByCompe(Long.parseLong(numerocompte.trim()), d1, d2));
                    } catch (NumberFormatException e) {
                        model.addAttribute("error", "Numero de compte invalide.");
                    }
                }
            }
        }
        return "marchand/operations-par-marchand";
    }

    /** Port of listecommicom.xhtml ("Liste des commissions des marchands"). */
    @GetMapping("/commissions")
    public String commissions(@RequestParam(required = false) String date1,
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
                model.addAttribute("results", marchandReportService.commissions(d1, d2));
            }
        }
        return "marchand/commissions";
    }

    private String renderOperations(String date1, String date2, Model model, String view) {
        model.addAttribute("date1", date1);
        model.addAttribute("date2", date2);
        if (WebDateUtils.hasBoth(date1, date2)) {
            Date d1 = WebDateUtils.parse(date1);
            Date d2 = WebDateUtils.parseEnd(date2);
            String erreurPeriode = WebDateUtils.erreurPeriode(d1, d2);
            if (erreurPeriode != null) {
                model.addAttribute("error", erreurPeriode);
            } else {
                model.addAttribute("results", marchandReportService.operations(d1, d2));
            }
        }
        return view;
    }
}
