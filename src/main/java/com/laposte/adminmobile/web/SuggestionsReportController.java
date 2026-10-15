package com.laposte.adminmobile.web;

import com.laposte.adminmobile.service.SuggestionsService;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Port of {@code listesugest.xhtml} ("Report Liste Suggestion"), the read-only date-filtered
 * report screen under Etats. Distinct from the CRUD "/suggestions" screens owned by another module
 * of this port. Reuses the already-ported {@link com.laposte.adminmobile.entities.Suggestions}
 * entity/repository - a solid, non-approximated query.
 */
@Controller
public class SuggestionsReportController {

    private final SuggestionsService suggestionsService;

    @Autowired
    public SuggestionsReportController(SuggestionsService suggestionsService) {
        this.suggestionsService = suggestionsService;
    }

    @GetMapping("/suggestions/liste")
    public String liste(@RequestParam(required = false) String date1,
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
                model.addAttribute("results", suggestionsService.findByPeriode(d1, d2));
            }
        }
        return "suggestions/liste";
    }
}
