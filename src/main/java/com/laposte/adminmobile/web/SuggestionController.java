package com.laposte.adminmobile.web;

import com.laposte.adminmobile.entities.Applimobile;
import com.laposte.adminmobile.entities.Suggestions;
import com.laposte.adminmobile.service.ApplimobileService;
import com.laposte.adminmobile.service.MailService;
import com.laposte.adminmobile.service.SuggestionsService;
import jakarta.validation.Valid;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Port of {@code sugestion.xhtml} (legacy {@code UserCtrl2#envoyeSugestion()}/{@code enregsugest()}).
 * Only the suggestion-creation piece of {@code UserCtrl2} is ported here, per the task brief - the
 * rest of that controller is owned by another module. The read-only date-filtered report screen
 * ("/suggestions/liste", port of {@code listesugest.xhtml}) is a separate module's
 * {@code SuggestionsReportController}; this controller adds a short "recent suggestions" recap
 * below the form for immediate feedback after submitting.
 */
@Controller
public class SuggestionController {

    private static final Logger log = LoggerFactory.getLogger(SuggestionController.class);

    /** Fixed notification recipient, as hardcoded in the legacy {@code UserCtrl2#envoyeSugestion()}. */
    private static final String NOTIFICATION_EMAIL = "kossi.adjo@laposte.tg";

    private final SuggestionsService suggestionsService;
    private final ApplimobileService applimobileService;
    private final MailService mailService;

    @Autowired
    public SuggestionController(SuggestionsService suggestionsService,
                                 ApplimobileService applimobileService,
                                 MailService mailService) {
        this.suggestionsService = suggestionsService;
        this.applimobileService = applimobileService;
        this.mailService = mailService;
    }

    @GetMapping("/suggestions")
    public String form(Model model) {
        model.addAttribute("suggestionForm", new SuggestionForm());
        populateReferenceData(model);
        return "suggestions/suggestion";
    }

    @PostMapping("/suggestions")
    public String create(@Valid @ModelAttribute("suggestionForm") SuggestionForm form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateReferenceData(model);
            return "suggestions/suggestion";
        }

        Suggestions s = new Suggestions();
        s.setTelClient(form.getTelephone());
        s.setLibellesugest(form.getMotif());
        s.setDatesugest(new Date());
        suggestionsService.create(s);

        String appliLibelle = "";
        if (form.getIdappli() != null) {
            Applimobile appli = applimobileService.find(form.getIdappli());
            if (appli != null) {
                appliLibelle = appli.getLibelleappli();
            }
        }
        try {
            mailService.sendMessage(
                    "Suggestion du client " + form.getTelephone() + " pour application " + appliLibelle,
                    form.getMotif(), NOTIFICATION_EMAIL);
        } catch (Exception e) {
            log.warn("Echec de l'envoi de la notification e-mail pour la suggestion {}", s.getIdsugest(), e);
        }

        redirectAttributes.addFlashAttribute("success", "Suggestion enregistree avec succes");
        return "redirect:/suggestions";
    }

    private void populateReferenceData(Model model) {
        model.addAttribute("listApplis", applimobileService.allApplication());
        model.addAttribute("recent", suggestionsService.allSugestionRecent());
    }
}
