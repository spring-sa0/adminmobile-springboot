package com.laposte.adminmobile.web;

import com.laposte.adminmobile.service.BankToBankReportService;
import com.laposte.adminmobile.service.RecuElectroniqueService;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Port of the "Etats &gt; Transactions" screens: {@code bw.xhtml}, {@code wb.xhtml},
 * {@code bb.xhtml} (covering {@code bbmoov.xhtml}, its FLooz-only variant), {@code cpteacpte.xhtml},
 * {@code retrait.xhtml}, {@code depot.xhtml}, {@code touttrans.xhtml}, {@code listeopecoccp.xhtml}
 * and {@code listeopannul.xhtml}. NB {@code bwflooz.xhtml} ("Bank To Wallet FLooz", despite the
 * task brief grouping it with bb) is - per its own title and the {@code EtatbeanCtrl} jasper file
 * names it calls ({@code bankwalletflooz.jasper}) - actually the FLooz-only variant of
 * bank-to-wallet, not bank-to-bank; it is folded into {@code bank-to-wallet}'s operateur filter
 * here instead, per the module instructions to use judgement based on what the source actually does.
 * <p>
 * IMPORTANT PROVENANCE NOTE: every one of these legacy screens (bb.xhtml excepted) rendered
 * nothing on-screen - they only triggered a JasperReports PDF/XLS/CSV export
 * ({@code EtatbeanCtrl.etat*excel/pdf/csv}), and the report's SQL lived inside the compiled
 * {@code .jasper} binary. Those binaries (and their {@code .jrxml} sources) are not present in the
 * legacy source tree for any of these screens except bank-to-bank ({@code banbankxls.jrxml} /
 * {@code banbankfloozxls.jrxml}, faithfully ported in {@link BankToBankReportService}), so the
 * exact filter semantics (which {@code code_operation} values mean "bank to wallet" vs "retrait"
 * vs "depot" etc) could not be recovered. Every method below except {@link #bankToBank} is
 * therefore a best-faith approximation built on {@link RecuElectroniqueService}: it applies the
 * date range and, where the legacy screen had an "Operateur" dropdown, the {@code origine} column
 * as a stand-in for that parameter. This returns a genuinely filterable, real result set, but the
 * date+operateur-only filtering will show a broader set of operations than the original,
 * report-specific `code_operation` filters would have - verify against production data before
 * relying on this for the withdrawal/deposit/bank-wallet distinctions specifically.
 */
@Controller
@RequestMapping("/etats/transactions")
public class TransactionEtatController {

    private final RecuElectroniqueService recuElectroniqueService;
    private final BankToBankReportService bankToBankReportService;
    private final PaginationEnBase paginationEnBase;

    @Autowired
    public TransactionEtatController(RecuElectroniqueService recuElectroniqueService,
                                      BankToBankReportService bankToBankReportService,
                                      PaginationEnBase paginationEnBase) {
        this.recuElectroniqueService = recuElectroniqueService;
        this.bankToBankReportService = bankToBankReportService;
        this.paginationEnBase = paginationEnBase;
    }

    @GetMapping("/bank-to-wallet")
    public String bankToWallet(@RequestParam(required = false) String date1,
                                @RequestParam(required = false) String date2,
                                @RequestParam(required = false) String operateur,
                                Model model) {
        return renderOperateurReport(date1, date2, operateur, model, "etats/transactions/bank-to-wallet");
    }

    @GetMapping("/wallet-to-bank")
    public String walletToBank(@RequestParam(required = false) String date1,
                                @RequestParam(required = false) String date2,
                                @RequestParam(required = false) String operateur,
                                Model model) {
        return renderOperateurReport(date1, date2, operateur, model, "etats/transactions/wallet-to-bank");
    }

    /** The one screen in this controller with a real, recovered SQL query - see class Javadoc. */
    @GetMapping("/bank-to-bank")
    public String bankToBank(@RequestParam(required = false) String date1,
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
                model.addAttribute("results", bankToBankReportService.findBankToBank(d1, d2, operateur));
            }
        }
        return "etats/transactions/bank-to-bank";
    }

    /**
     * Port of cpteacpte.xhtml ("Compte a compte Eposte"). No operateur field on this legacy
     * screen; scoped to Eposte-subscriber telephones (see repository-level TODO on
     * {@code findEposteScopedOperationsBetween}) as the closest available approximation of
     * "compte a compte" transfers.
     */
    @GetMapping("/compte-a-compte")
    public String compteACompte(@RequestParam(required = false) String date1,
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
                model.addAttribute("results", paginationEnBase.charger(page -> recuElectroniqueService.recupListopEposte(d1, d2, page), model));
            }
        }
        return "etats/transactions/compte-a-compte";
    }

    @GetMapping("/retrait")
    public String retrait(@RequestParam(required = false) String date1,
                           @RequestParam(required = false) String date2,
                           @RequestParam(required = false) String operateur,
                           Model model) {
        return renderOperateurReport(date1, date2, operateur, model, "etats/transactions/retrait");
    }

    @GetMapping("/depot")
    public String depot(@RequestParam(required = false) String date1,
                         @RequestParam(required = false) String date2,
                         @RequestParam(required = false) String operateur,
                         Model model) {
        return renderOperateurReport(date1, date2, operateur, model, "etats/transactions/depot");
    }

    /** Port of touttrans.xhtml ("Report Global"): all operations, optionally narrowed by operateur. */
    @GetMapping("/global")
    public String global(@RequestParam(required = false) String date1,
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
        return "etats/transactions/global";
    }

    /** Port of listeopecoccp.xhtml: every EcoCCP receipt in the period - a solid, non-approximated query. */
    @GetMapping("/ecoccp")
    public String ecoccp(@RequestParam(required = false) String date1,
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
                model.addAttribute("results", paginationEnBase.charger(page -> recuElectroniqueService.recupListopParPeriode(d1, d2, page), model));
            }
        }
        return "etats/transactions/ecoccp";
    }

    /** Port of listeopannul.xhtml: cancelled EcoCCP operations - a solid, non-approximated query. */
    @GetMapping("/ecoccp-annulees")
    public String ecoccpAnnulees(@RequestParam(required = false) String date1,
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
                model.addAttribute("results", paginationEnBase.charger(page -> recuElectroniqueService.recupListopAnnulees(d1, d2, page), model));
            }
        }
        return "etats/transactions/ecoccp-annulees";
    }

    private String renderOperateurReport(String date1, String date2, String operateur, Model model, String view) {
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
        return view;
    }
}
