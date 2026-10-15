package com.laposte.adminmobile.web;

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
 * Port of the "Etats &gt; Eposte" operations screens: {@code opeposteclt.xhtml},
 * {@code opepostecpt.xhtml}, {@code eposteopfait.xhtml}, {@code eposteoppasfait.xhtml} and
 * {@code listeopeposte.xhtml}. Same provenance caveat as {@link TransactionEtatController}: these
 * legacy screens only triggered a Jasper export with no recoverable SQL. {@code operations-client}
 * and {@code operations-compte} reuse the exact telephone/compte+period queries
 * {@code RecuElectroniqueService}/{@code OpCtrl} already expose (a phone number or account number
 * already narrows to one specific holder regardless of channel, so no further Eposte-specific
 * scoping is needed there). {@code comptes-op-effectuees}/{@code comptes-op-non-effectuees} are a
 * genuine, non-approximated "did this Eposte account transact in the period" query against
 * {@code USERS_EPOSTE} joined by telephone to the EcoCCP receipt table. {@code operations} (the
 * "Liste des Operations Eposte" screen) is the same best-faith Eposte-subscriber scoping used by
 * {@code /etats/transactions/compte-a-compte} - see {@code RecuElectroniqueRepository}.
 */
@Controller
@RequestMapping("/eposte")
public class EposteOperationController {

    private final RecuElectroniqueService recuElectroniqueService;
    private final UsersService usersService;

    @Autowired
    public EposteOperationController(RecuElectroniqueService recuElectroniqueService, UsersService usersService) {
        this.recuElectroniqueService = recuElectroniqueService;
        this.usersService = usersService;
    }

    /** Port of opeposteclt.xhtml. */
    @GetMapping("/operations-client")
    public String operationsClient(@RequestParam(required = false) String date1,
                                    @RequestParam(required = false) String date2,
                                    @RequestParam(required = false) String telephone,
                                    Model model) {
        model.addAttribute("date1", date1);
        model.addAttribute("date2", date2);
        model.addAttribute("telephone", telephone);
        if (!WebDateUtils.isBlank(telephone)) {
            if (!WebDateUtils.hasBoth(date1, date2)) {
                model.addAttribute("error", "Veuillez renseigner les deux dates.");
            } else {
                Date d1 = WebDateUtils.parse(date1);
                Date d2 = WebDateUtils.parseEnd(date2);
                String erreurPeriode = WebDateUtils.erreurPeriode(d1, d2);
                if (erreurPeriode != null) {
                    model.addAttribute("error", erreurPeriode);
                } else {
                    model.addAttribute("results",
                            recuElectroniqueService.recupListopByTelephone("228" + telephone.trim(), d1, d2));
                }
            }
        }
        return "eposte/operations-client";
    }

    /** Port of opepostecpt.xhtml. */
    @GetMapping("/operations-compte")
    public String operationsCompte(@RequestParam(required = false) String date1,
                                    @RequestParam(required = false) String date2,
                                    @RequestParam(required = false) String numerocompte,
                                    Model model) {
        model.addAttribute("date1", date1);
        model.addAttribute("date2", date2);
        model.addAttribute("numerocompte", numerocompte);
        if (!WebDateUtils.isBlank(numerocompte)) {
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
        return "eposte/operations-compte";
    }

    /** Port of eposteopfait.xhtml. */
    @GetMapping("/comptes-op-effectuees")
    public String comptesOpEffectuees(@RequestParam(required = false) String date1,
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
                model.addAttribute("results", usersService.usersWithOperationsBetween(d1, d2));
            }
        }
        return "eposte/comptes-op-effectuees";
    }

    /** Port of eposteoppasfait.xhtml. */
    @GetMapping("/comptes-op-non-effectuees")
    public String comptesOpNonEffectuees(@RequestParam(required = false) String date1,
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
                model.addAttribute("results", usersService.usersWithoutOperationsBetween(d1, d2));
            }
        }
        return "eposte/comptes-op-non-effectuees";
    }

    /** Port of listeopeposte.xhtml. */
    @GetMapping("/operations")
    public String operations(@RequestParam(required = false) String date1,
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
                model.addAttribute("results", recuElectroniqueService.recupListopEposte(d1, d2));
            }
        }
        return "eposte/operations";
    }
}
