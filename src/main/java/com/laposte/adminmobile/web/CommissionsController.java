package com.laposte.adminmobile.web;

import com.laposte.adminmobile.service.CommissionsReportService;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Port of {@code commissions.xhtml} ("Commissions EcoCCP") and {@code comeposte.xhtml}
 * ("Commissions EPOSTE"). See {@link com.laposte.adminmobile.repository.CommissionsReportRepository}
 * for provenance and the operateur-label-mismatch TODO.
 */
@Controller
@RequestMapping("/commissions")
public class CommissionsController {

    private final CommissionsReportService commissionsReportService;

    @Autowired
    public CommissionsController(CommissionsReportService commissionsReportService) {
        this.commissionsReportService = commissionsReportService;
    }

    /** Port of commissions.xhtml - operateur choices TOGOCEL/MOOV. */
    @GetMapping("/ecoccp")
    public String ecoccp(@RequestParam(required = false) String date1,
                          @RequestParam(required = false) String date2,
                          @RequestParam(required = false) String operateur,
                          Model model) {
        return render(date1, date2, operateur, model, "commissions/ecoccp");
    }

    /** Port of comeposte.xhtml - operateur choices TMoney/FLooz. */
    @GetMapping("/eposte")
    public String eposte(@RequestParam(required = false) String date1,
                          @RequestParam(required = false) String date2,
                          @RequestParam(required = false) String operateur,
                          Model model) {
        return render(date1, date2, operateur, model, "commissions/eposte");
    }

    private String render(String date1, String date2, String operateur, Model model, String view) {
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
                model.addAttribute("results", commissionsReportService.commissionsSummary(d1, d2, operateur));
            }
        }
        return view;
    }
}
