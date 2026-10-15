package com.laposte.adminmobile.web;

import com.laposte.adminmobile.entities.Releve;
import com.laposte.adminmobile.service.FormatsService;
import com.laposte.adminmobile.service.ReleveGridExportService;
import com.laposte.adminmobile.service.ReleveService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

/**
 * Port of admin.mobile.ctrl.ReleveCtrl (web/releveliste.xhtml and web/releveauto_new.xhtml): plain
 * CRUD over the RELEVE table, which holds account-statement scheduling parameters (account, mail,
 * frequency, file format) - these rows are NOT generated report files themselves. Actual statement
 * generation/mailing on that schedule is presumably handled by a separate batch/mailer process
 * outside this webapp's source tree (see ReleveExportController's class javadoc); this controller
 * only manages the schedule rows, plus the on-demand exports live in ReleveExportController.
 *
 * <p>web/releveauto.xhtml (single dropdown selection) is superseded here by releveauto_new.xhtml's
 * checkbox-matrix screen per the module brief - both ultimately call the same
 * ReleveCtrl.enregReleve()/getReleveEnreg() duplicate-check logic, so releveauto_new is a strict
 * superset and releveauto itself was not separately ported.
 */
@Controller
@RequestMapping("/releves")
public class ReleveController {

    private final ReleveService releveService;
    private final FormatsService formatsService;
    private final ReleveGridExportService releveGridExportService;

    public ReleveController(ReleveService releveService,
                             FormatsService formatsService,
                             ReleveGridExportService releveGridExportService) {
        this.releveService = releveService;
        this.formatsService = formatsService;
        this.releveGridExportService = releveGridExportService;
    }

    // ---------- Liste (releveliste.xhtml) ----------

    @GetMapping("/liste")
    public String liste(Model model) {
        model.addAttribute("listereleve", releveService.allReleve());
        return "releve-liste";
    }

    /** Server-side equivalent of releveliste.xhtml's p:dataExporter type="xls" on the grid. */
    @GetMapping("/liste/export")
    public ResponseEntity<byte[]> exportListe() throws IOException {
        byte[] bytes = releveGridExportService.exportReleveGrid(releveService.allReleve());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Listereleve.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/liste/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        Releve releve = releveService.find(id);
        if (releve == null) {
            return "redirect:/releves/liste";
        }
        model.addAttribute("releve", releve);
        return "releve-liste-edit";
    }

    /** Seuls les champs du formulaire d'edition sont liables (l'id vient du chemin). */
    @InitBinder("releve")
    void restrictFields(WebDataBinder binder) {
        binder.setAllowedFields("numerocompte", "intitule", "mail", "jourenvoi", "typefichier");
    }

    /** Port of ReleveCtrl.onRowEdit(Releve): entityManager.merge() via ReleveService.edit(). */
    @PostMapping("/liste/{id}/edit")
    public String edit(@PathVariable Integer id, @ModelAttribute Releve releve, RedirectAttributes redirectAttributes) {
        // merge() d'un id inexistant creerait une nouvelle ligne au lieu d'echouer.
        if (releveService.find(id) == null) {
            return "redirect:/releves/liste";
        }
        releve.setIdreleve(id);
        releveService.edit(releve);
        redirectAttributes.addFlashAttribute("message", "Modification effectuee avec succes");
        return "redirect:/releves/liste";
    }

    /** Port of ReleveCtrl.delete(Releve). */
    @PostMapping("/liste/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Releve releve = releveService.find(id);
        if (releve != null) {
            releveService.delete(releve);
            redirectAttributes.addFlashAttribute("message", "Suppression effectuee avec succes");
        }
        return "redirect:/releves/liste";
    }

    // ---------- Parametrage (releveauto_new.xhtml) ----------

    @GetMapping("/parametrage")
    public String parametrageForm(Model model) {
        if (!model.containsAttribute("parametrageForm")) {
            model.addAttribute("parametrageForm", new ReleveParametrageForm());
        }
        return "releve-parametrage";
    }

    /**
     * Port of ReleveCtrl.enrege(): for every non-blank email in the comma-separated list, create
     * one RELEVE row per checked (frequence x format) combination, skipping any combination already
     * configured for this account (ReleveCtrl.enregReleve()'s getReleveEnreg() duplicate check).
     */
    @PostMapping("/parametrage")
    public String parametrage(@ModelAttribute ReleveParametrageForm form, RedirectAttributes redirectAttributes) {
        if (form.getNumerocompte() == null || form.getNumerocompte().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Veuillez entrer le numero de compte");
            redirectAttributes.addFlashAttribute("parametrageForm", form);
            return "redirect:/releves/parametrage";
        }
        if (form.getEmail() == null || form.getEmail().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "L'adresse mail ne doit pas etre nulle");
            redirectAttributes.addFlashAttribute("parametrageForm", form);
            return "redirect:/releves/parametrage";
        }

        Long numerocompte;
        try {
            numerocompte = Long.valueOf(form.getNumerocompte().trim());
        } catch (NumberFormatException ex) {
            redirectAttributes.addFlashAttribute("error", "Numero de compte invalide");
            redirectAttributes.addFlashAttribute("parametrageForm", form);
            return "redirect:/releves/parametrage";
        }

        List<String> frequences = form.selectedFrequences();
        List<String> formats = form.selectedFormats();
        if (frequences.isEmpty() || formats.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Veuillez selectionner au moins une frequence et un format");
            redirectAttributes.addFlashAttribute("parametrageForm", form);
            return "redirect:/releves/parametrage";
        }

        String intitule = formatsService.getIntitule(form.getNumerocompte().trim());

        int created = 0;
        int skipped = 0;
        for (String rawMail : form.getEmail().split(",")) {
            String mail = rawMail.trim();
            if (mail.isEmpty()) {
                continue;
            }
            for (String frequence : frequences) {
                for (String format : formats) {
                    if (!releveService.getReleveEnreg(numerocompte, frequence, mail, format).isEmpty()) {
                        skipped++;
                        continue;
                    }
                    Releve releve = new Releve();
                    releve.setNumerocompte(numerocompte);
                    releve.setJourenvoi(frequence);
                    releve.setMail(mail);
                    releve.setTypefichier(format);
                    releve.setIntitule(intitule);
                    releveService.create(releve);
                    created++;
                }
            }
        }

        if (created > 0) {
            redirectAttributes.addFlashAttribute("message",
                    "Parametrage effectue avec succes (" + created + " ligne(s) creee(s)"
                            + (skipped > 0 ? ", " + skipped + " deja existante(s)" : "") + ")");
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "Ce compte est deja parametre pour tous les formats/frequences/mails choisis");
        }
        return "redirect:/releves/parametrage";
    }
}
