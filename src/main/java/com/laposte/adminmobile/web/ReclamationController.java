package com.laposte.adminmobile.web;

import com.laposte.adminmobile.entities.Applimobile;
import com.laposte.adminmobile.entities.EtatReclamation;
import com.laposte.adminmobile.entities.Reclamations;
import com.laposte.adminmobile.entities.Support;
import com.laposte.adminmobile.entities.TypeReclammation;
import com.laposte.adminmobile.service.ApplimobileService;
import com.laposte.adminmobile.service.MailService;
import com.laposte.adminmobile.service.ReclamationsService;
import com.laposte.adminmobile.service.SupportService;
import com.laposte.adminmobile.service.TypeReclammationService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Port of {@code reclamation.xhtml} (creation + the "non resolues" recap list),
 * {@code listerecla.xhtml} (period/etat report), {@code affectrecla.xhtml} (assignment) and
 * {@code clotureRecla.xhtml} (closing / sending back). Legacy controllers: creation came from
 * {@code UserCtrl2#envoyeRecla()}/{@code enregRecla()}; assignment/closing/rendre from
 * {@code ReclaCtrl}.
 */
@Controller
public class ReclamationController {

    private static final Logger log = LoggerFactory.getLogger(ReclamationController.class);

    /** Fixed notification recipient, as hardcoded in the legacy {@code UserCtrl2#envoyeRecla()}. */
    private static final String NOTIFICATION_EMAIL = "ecoccp@laposte.tg";

    private final ReclamationsService reclamationsService;
    private final TypeReclammationService typeReclammationService;
    private final ApplimobileService applimobileService;
    private final SupportService supportService;
    private final MailService mailService;

    @Autowired
    public ReclamationController(ReclamationsService reclamationsService,
                                  TypeReclammationService typeReclammationService,
                                  ApplimobileService applimobileService,
                                  SupportService supportService,
                                  MailService mailService) {
        this.reclamationsService = reclamationsService;
        this.typeReclammationService = typeReclammationService;
        this.applimobileService = applimobileService;
        this.supportService = supportService;
        this.mailService = mailService;
    }

    // ---- reclamation.xhtml : creation + liste des reclamations non resolues ----

    @GetMapping("/reclamations")
    public String form(Model model) {
        model.addAttribute("reclamationForm", new ReclamationForm());
        populateReferenceData(model);
        return "reclamations/reclamation";
    }

    @PostMapping("/reclamations")
    public String create(@Valid @ModelAttribute("reclamationForm") ReclamationForm form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        TypeReclammation type = null;
        if (form.getIdtype() != null) {
            type = typeReclammationService.find(form.getIdtype());
            if (type == null) {
                bindingResult.rejectValue("idtype", "invalid", "Type de reclamation invalide");
            }
        }
        if (bindingResult.hasErrors()) {
            populateReferenceData(model);
            return "reclamations/reclamation";
        }

        Reclamations r = new Reclamations();
        r.setTelClient(form.getTelephone());
        r.setMotifreclamatin(form.getMotif());
        r.setDatereclamation(new Date());
        r.setIdtype(type);
        // NR = "non reglee", exactly like UserCtrl2#enregRecla().
        r.setIdetat(new EtatReclamation("NR"));
        reclamationsService.create(r);

        String appliLibelle = "";
        if (form.getIdappli() != null) {
            Applimobile appli = applimobileService.find(form.getIdappli());
            if (appli != null) {
                appliLibelle = appli.getLibelleappli();
            }
        }
        try {
            mailService.sendMessage(
                    "Reclamation du client " + form.getTelephone() + " pour application " + appliLibelle,
                    form.getMotif(), NOTIFICATION_EMAIL);
        } catch (Exception e) {
            // Do not fail the whole request just because the SMTP relay is unreachable/unconfigured
            // (see MailService TODO) - the complaint itself is already recorded at this point.
            log.warn("Echec de l'envoi de la notification e-mail pour la reclamation {}", r.getIdreclamation(), e);
        }

        redirectAttributes.addFlashAttribute("success", "Reclamation enregistree avec succes");
        return "redirect:/reclamations";
    }

    private void populateReferenceData(Model model) {
        model.addAttribute("listTypes", typeReclammationService.allTypeRecla());
        model.addAttribute("listApplis", applimobileService.allApplication());
        model.addAttribute("openList", reclamationsService.getReclamationNR());
    }

    // ---- listerecla.xhtml : report filtre par periode / etat ----
    // The legacy screen (EtatbeanCtrl) only offered PDF/Excel exports of this query - no
    // Jasper/POI reporting is set up in this port, so this shows the same data on-screen instead.

    @GetMapping("/reclamations/liste")
    public String liste(@RequestParam(required = false) String date1,
                         @RequestParam(required = false) String date2,
                         @RequestParam(required = false) String etat,
                         Model model) {
        model.addAttribute("date1", date1);
        model.addAttribute("date2", date2);
        model.addAttribute("etat", etat);
        Date d1 = WebDateUtils.parse(date1);
        Date d2 = WebDateUtils.parse(date2);
        if ((!WebDateUtils.isBlank(date1) && d1 == null) || (!WebDateUtils.isBlank(date2) && d2 == null)) {
            model.addAttribute("error", "Dates invalides.");
        } else {
            model.addAttribute("results", reclamationsService.getReportList(etat, d1, d2));
        }
        return "reclamations/liste";
    }

    // ---- affectrecla.xhtml ----

    @GetMapping("/reclamations/affectation")
    public String affectationForm(Model model) {
        model.addAttribute("openList", reclamationsService.getReclamationNR());
        model.addAttribute("listSupport", supportService.allSupport());
        return "reclamations/affectation";
    }

    /** Port of {@code ReclaCtrl#affecter(Reclamations)}. */
    @PostMapping("/reclamations/affectation")
    public String affecter(@RequestParam BigDecimal idreclamation,
                            @RequestParam BigDecimal idsupport,
                            RedirectAttributes redirectAttributes) {
        Reclamations r = reclamationsService.find(idreclamation);
        Support support = supportService.find(idsupport);
        if (r == null || support == null) {
            redirectAttributes.addFlashAttribute("error", "Reclamation ou support introuvable.");
            return "redirect:/reclamations/affectation";
        }
        r.setIdetat(new EtatReclamation("A"));
        r.setIdsupport(support);
        r.setDateaffect(new Date());
        reclamationsService.edit(r);

        try {
            String typeLibelle = r.getIdtype() != null ? r.getIdtype().getLibellerecla() : "";
            mailService.sendMessage(typeLibelle, r.getMotifreclamatin(), support.getMailsupport());
        } catch (Exception e) {
            log.warn("Echec de l'envoi de la notification e-mail d'affectation pour la reclamation {}", idreclamation, e);
        }

        redirectAttributes.addFlashAttribute("success", "Affectation effectuee avec succes!");
        return "redirect:/reclamations/affectation";
    }

    // ---- clotureRecla.xhtml ----

    @GetMapping("/reclamations/cloture")
    public String clotureForm(Authentication authentication, Model model) {
        String codeutilisateur = authentication != null ? authentication.getName() : null;
        model.addAttribute("openList", reclamationsService.getReclaBySupport(codeutilisateur));
        return "reclamations/cloture";
    }

    /** Port of {@code ReclaCtrl#cloturer(Reclamations)}. */
    @PostMapping("/reclamations/cloture")
    public String cloturer(@RequestParam BigDecimal idreclamation,
                            @RequestParam(required = false) String remarques,
                            RedirectAttributes redirectAttributes) {
        Reclamations r = reclamationsService.find(idreclamation);
        if (r == null) {
            redirectAttributes.addFlashAttribute("error", "Reclamation introuvable.");
            return "redirect:/reclamations/cloture";
        }
        r.setIdetat(new EtatReclamation("R"));
        r.setRemarques(remarques);
        r.setDatecloture(new Date());
        reclamationsService.edit(r);
        redirectAttributes.addFlashAttribute("success", "Cloture effectuee avec succes!");
        return "redirect:/reclamations/cloture";
    }

    /**
     * Port of {@code ReclaCtrl#rendreRecla(Reclamations)}: sends the complaint back to état "NR"
     * (non affectee). Same screen as cloturer() above (clotureRecla.xhtml has both a "Cloturer"
     * and a "Rendre" button per row) - kept as a distinct sub-path to avoid ambiguity with the
     * action above. Faithfully preserves the legacy quirk of leaving idsupport/dateaffect
     * untouched when reverting.
     */
    @PostMapping("/reclamations/cloture/rendre")
    public String rendre(@RequestParam BigDecimal idreclamation,
                          RedirectAttributes redirectAttributes) {
        Reclamations r = reclamationsService.find(idreclamation);
        if (r == null) {
            redirectAttributes.addFlashAttribute("error", "Reclamation introuvable.");
            return "redirect:/reclamations/cloture";
        }
        r.setIdetat(new EtatReclamation("NR"));
        reclamationsService.edit(r);
        redirectAttributes.addFlashAttribute("success", "Affectation transferee avec succes!");
        return "redirect:/reclamations/cloture";
    }
}
