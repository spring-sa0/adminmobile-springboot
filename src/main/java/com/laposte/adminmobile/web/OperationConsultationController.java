package com.laposte.adminmobile.web;

import com.laposte.adminmobile.entities.RecuElectronique;
import com.laposte.adminmobile.service.OperationService;
import com.laposte.adminmobile.service.RecuElectroniqueService;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Port of {@code consultaionop.xhtml}, {@code consultop.xhtml}, {@code listeop.xhtml},
 * {@code opparclient.xhtml} and {@code opparcompte.xhtml}, backed respectively by
 * {@code OpCtrl.afficherop/recherherop/afficherlisteop} and (for the last two, which had no
 * on-screen table in the legacy app - see class-level TODO) the same {@code RecuElectroniqueService}
 * telephone/compte queries {@code OpCtrl} itself uses.
 */
@Controller
public class OperationConsultationController {

    private final RecuElectroniqueService recuElectroniqueService;
    private final OperationService operationService;

    @Autowired
    public OperationConsultationController(RecuElectroniqueService recuElectroniqueService,
                                            OperationService operationService) {
        this.recuElectroniqueService = recuElectroniqueService;
        this.operationService = operationService;
    }

    /** Port of consultaionop.xhtml / OpCtrl#afficherop(). */
    @GetMapping("/consultations/operations")
    public String operations(@RequestParam(required = false) String ptelephone,
                              @RequestParam(required = false) String numcompte,
                              @RequestParam(required = false) String date1,
                              @RequestParam(required = false) String date2,
                              Model model) {
        model.addAttribute("ptelephone", ptelephone);
        model.addAttribute("numcompte", numcompte);
        model.addAttribute("date1", date1);
        model.addAttribute("date2", date2);

        boolean hasTel = !WebDateUtils.isBlank(ptelephone);
        boolean hasCompte = !WebDateUtils.isBlank(numcompte);
        if (hasTel || hasCompte) {
            if (!WebDateUtils.hasBoth(date1, date2)) {
                model.addAttribute("error", "Veuillez renseigner les deux dates.");
            } else {
                Date d1 = WebDateUtils.parse(date1);
                Date d2 = WebDateUtils.parse(date2);
                if (d1 == null || d2 == null) {
                    model.addAttribute("error", "Dates invalides.");
                } else if (!hasTel) {
                    // Legacy afficherop(): telephone blank -> search by compte.
                    model.addAttribute("results", parseCompteAndSearch(numcompte, d1, d2, model));
                } else {
                    // Legacy afficherop(): telephone present -> search by telephone (with or
                    // without a compte also filled in - the legacy code searches by telephone
                    // either way), country code "228" prefixed exactly as OpCtrl does.
                    model.addAttribute("results",
                            recuElectroniqueService.recupListopByTelephone("228" + ptelephone.trim(), d1, d2));
                }
            }
        }
        return "consultations/operations";
    }

    private List<RecuElectronique> parseCompteAndSearch(String numcompte, Date d1, Date d2, Model model) {
        try {
            return recuElectroniqueService.recupListopByCompe(Long.parseLong(numcompte.trim()), d1, d2);
        } catch (NumberFormatException e) {
            model.addAttribute("error", "Numero de compte invalide.");
            return List.of();
        }
    }

    /**
     * Port of consultop.xhtml / OpCtrl#recherherop(). TODO: the legacy method's third branch
     * ("both provided") tested the session-scoped {@code ptelephone} field left over from the
     * consultaionop screen (OpCtrl is @SessionScoped and shared across screens) rather than a
     * field on this form - almost certainly a copy/paste bug, and not reproducible in a stateless
     * per-request controller. Simplified here to: numeroop wins when present, else referenceid -
     * which matches every legacy branch's actual outcome except that unreachable-in-practice bug.
     */
    @GetMapping("/consultations/operation")
    public String operation(@RequestParam(required = false) String numeroop,
                             @RequestParam(required = false) String referenceid,
                             Model model) {
        model.addAttribute("numeroop", numeroop);
        model.addAttribute("referenceid", referenceid);

        boolean hasNumop = !WebDateUtils.isBlank(numeroop);
        boolean hasRef = !WebDateUtils.isBlank(referenceid);
        if (hasNumop) {
            try {
                model.addAttribute("results", recuElectroniqueService.recupListopBynumop(Long.parseLong(numeroop.trim())));
            } catch (NumberFormatException e) {
                model.addAttribute("error", "Numero d'operation invalide.");
            }
        } else if (hasRef) {
            model.addAttribute("results", recuElectroniqueService.recupListopByRefid(referenceid.trim()));
        }
        return "consultations/operation";
    }

    /** Port of listeop.xhtml / OpCtrl#afficherlisteop(). */
    @GetMapping("/operations/liste")
    public String liste(@RequestParam(required = false) String date1,
                         @RequestParam(required = false) String date2,
                         @RequestParam(required = false) Integer codeOperation,
                         Model model) {
        model.addAttribute("date1", date1);
        model.addAttribute("date2", date2);
        model.addAttribute("codeOperation", codeOperation);
        model.addAttribute("typesOperation", operationService.allOp());

        if (WebDateUtils.hasBoth(date1, date2) && codeOperation != null) {
            Date d1 = WebDateUtils.parse(date1);
            Date d2 = WebDateUtils.parse(date2);
            if (d1 == null || d2 == null) {
                model.addAttribute("error", "Dates invalides.");
            } else {
                model.addAttribute("results", recuElectroniqueService.recupListop(d1, d2, codeOperation.toString()));
            }
        }
        return "operations/liste";
    }

    /**
     * Port of opparclient.xhtml. The legacy screen only exported to Jasper (no on-screen table -
     * see module-level TODO in TransactionEtatController); reuses the same telephone+period query
     * OpCtrl itself runs for consultaionop, "228"-prefixed the same way.
     */
    @GetMapping("/operations/par-client")
    public String parClient(@RequestParam(required = false) String date1,
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
                Date d2 = WebDateUtils.parse(date2);
                if (d1 == null || d2 == null) {
                    model.addAttribute("error", "Dates invalides.");
                } else {
                    model.addAttribute("results",
                            recuElectroniqueService.recupListopByTelephone("228" + telephone.trim(), d1, d2));
                }
            }
        }
        return "operations/par-client";
    }

    /** Port of opparcompte.xhtml - see opparclient note above. */
    @GetMapping("/operations/par-compte")
    public String parCompte(@RequestParam(required = false) String date1,
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
                Date d2 = WebDateUtils.parse(date2);
                if (d1 == null || d2 == null) {
                    model.addAttribute("error", "Dates invalides.");
                } else {
                    model.addAttribute("results", parseCompteAndSearch(numerocompte, d1, d2, model));
                }
            }
        }
        return "operations/par-compte";
    }
}
