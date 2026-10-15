package com.laposte.adminmobile.web;

import com.laposte.adminmobile.service.FormatsService;
import com.laposte.adminmobile.service.JasperReportService;
import com.laposte.adminmobile.service.ReleveExportService;
import net.sf.jasperreports.engine.JasperPrint;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of the two "releve" (account statement) export screens that, in the legacy app, are driven
 * by admin.mobile.ctrl.EtatbeanCtrl rather than ReleveCtrl:
 * <ul>
 *   <li>web/relevexcel.xhtml -&gt; GET/POST /releves/excel-afb120
 *     <ul>
 *       <li>"Export excel" button -&gt; EtatbeanCtrl.etatrelevexcel() -&gt; reports/releve_client1.jrxml (.xls)</li>
 *       <li>"Export AFB120" button -&gt; EtatbeanCtrl.fichierafbtext() -&gt; plain-text AFB120 flat file (no Jasper report)</li>
 *     </ul>
 *   </li>
 *   <li>web/relevetgcel.xhtml -&gt; GET/POST /releves/excel-togocel
 *     <ul>
 *       <li>"Export excel" button -&gt; EtatbeanCtrl.etatrelevetgcel() -&gt; reports/releve_solde.jrxml (.xls)</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p><b>Report choice - deviation from the module brief:</b> the task brief for this module
 * suggested relevexcel/relevetgcel rendered banbankxls.jrxml/banbankfloozxls.jrxml/op_togocel.jrxml.
 * Reading the actual legacy wiring - relevexcel.xhtml's and relevetgcel.xhtml's command buttons
 * call {@code etatbeanCtrl.etatrelevexcel()}/{@code etatrelevetgcel()}, never anything touching
 * banbankop_togocel - shows those two screens actually render releve_client1.jrxml and
 * releve_solde.jrxml. The three banbank op_togocel reports are used elsewhere in EtatbeanCtrl
 * (separate bank-to-bank / TOGOCEL *operation listing* screens under the "Etats" menu, not part of
 * this module and not yet ported) and are not reachable from either page here. Ported per the
 * verified actual behaviour; the three unused .jrxml files stay in resources/reports/ for whichever
 * module later ports those other screens.
 *
 * <p>Also note: releve.jrxml, releve_auto.jrxml and releve_client.jrxml (also pre-copied into
 * resources/reports/) are not referenced by ANY .java file in the legacy source tree that was
 * ported here - no controller loads them. They are presumably consumed by a separate batch/mailer
 * process (outside this webapp's source) that actually generates and emails the statements on the
 * schedule configured via /releves/parametrage. They are intentionally left unwired.
 */
@Controller
@RequestMapping("/releves")
public class ReleveExportController {

    private final JasperReportService jasperReportService;
    private final ReleveExportService releveExportService;
    private final FormatsService formatsService;

    public ReleveExportController(JasperReportService jasperReportService,
                                   ReleveExportService releveExportService,
                                   FormatsService formatsService) {
        this.jasperReportService = jasperReportService;
        this.releveExportService = releveExportService;
        this.formatsService = formatsService;
    }

    // ---------- Excel / AFB120 (relevexcel.xhtml) ----------

    @GetMapping("/excel-afb120")
    public String excelAfb120Form(Model model) {
        if (!model.containsAttribute("releveExportForm")) {
            model.addAttribute("releveExportForm", new ReleveExportForm());
        }
        return "releve-excel-afb120";
    }

    /** Query-driven report: releve_client1.jrxml's own SELECT reads IMOBILE.MOUVEMENT_UL after populateReleveClient1() fills it. */
    @PostMapping("/excel-afb120/excel")
    public Object excelAfb120Excel(@ModelAttribute ReleveExportForm form, RedirectAttributes redirectAttributes) {
        String invalid = rejectIfIncomplete(form, "/releves/excel-afb120", redirectAttributes);
        if (invalid != null) {
            return invalid;
        }
        releveExportService.populateReleveClient1(form.getNumerocompte(), form.getDatedebut(), form.getDatefin());

        Map<String, Object> params = new HashMap<>();
        params.put("numcompte", form.getNumerocompte());
        JasperPrint jasperPrint = jasperReportService.fillFromQuery("releve_client1", params);
        byte[] bytes = jasperReportService.toXls(jasperPrint);

        return excelResponse(bytes, "releve_client1.xls");
    }

    /** Not a Jasper report: replicates EtatbeanCtrl.FichierAFB() - populate then dump IMOBILE.MODELEAFB as flat text. */
    @PostMapping("/excel-afb120/afb120")
    public Object excelAfb120Text(@ModelAttribute ReleveExportForm form, RedirectAttributes redirectAttributes) {
        String invalid = rejectIfIncomplete(form, "/releves/excel-afb120", redirectAttributes);
        if (invalid != null) {
            return invalid;
        }
        releveExportService.populateAfb120(form.getNumerocompte(), form.getDatedebut(), form.getDatefin());
        List<String> lines = releveExportService.fetchAfb120Lines();
        String content = String.join(System.lineSeparator(), lines);
        byte[] bytes = content.getBytes(StandardCharsets.ISO_8859_1);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"releve_afb120.txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
    }

    // ---------- Excel / TOGOCEL (relevetgcel.xhtml) ----------

    @GetMapping("/excel-togocel")
    public String excelTogocelForm(Model model) {
        if (!model.containsAttribute("releveExportForm")) {
            model.addAttribute("releveExportForm", new ReleveExportForm());
        }
        return "releve-excel-togocel";
    }

    /** Query-driven report: releve_solde.jrxml's own SELECT reads IMOBILE.MOUVEMENT after populateReleveSolde() fills it. */
    @PostMapping("/excel-togocel/excel")
    public Object excelTogocel(@ModelAttribute ReleveExportForm form, RedirectAttributes redirectAttributes) {
        String invalid = rejectIfIncomplete(form, "/releves/excel-togocel", redirectAttributes);
        if (invalid != null) {
            return invalid;
        }
        releveExportService.populateReleveSolde(form.getNumerocompte(), form.getDatedebut(), form.getDatefin());

        Map<String, Object> params = new HashMap<>();
        params.put("numcompte", form.getNumerocompte());
        JasperPrint jasperPrint = jasperReportService.fillFromQuery("releve_solde", params);
        byte[] bytes = jasperReportService.toXls(jasperPrint);

        return excelResponse(bytes, "releve_solde.xls");
    }

    // ---------- Shared "intitule" lookup, ported from EtatbeanCtrl.intitulecompte() (p:ajax on numcpt change) ----------

    // text/plain : la valeur vient de la base et ne doit jamais etre interpretee comme du HTML.
    @GetMapping(value = "/intitule", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String intitule(@RequestParam String numerocompte) {
        String intitule = formatsService.getIntitule(numerocompte);
        return intitule != null ? intitule : "";
    }

    /**
     * The Oracle procedures behind these exports are called with whatever the form sent: without
     * this check a missing compte/date reached them as NULL and ended in a 500 error page.
     * Returns the redirect back to the form, or null when the form is complete.
     */
    private static String rejectIfIncomplete(ReleveExportForm form, String formUrl, RedirectAttributes redirectAttributes) {
        String error = null;
        if (form.getNumerocompte() == null || form.getNumerocompte().isBlank()) {
            error = "Veuillez entrer le numero de compte";
        } else if (form.getDatedebut() == null || form.getDatefin() == null) {
            error = "Veuillez renseigner les deux dates";
        } else if (form.getDatedebut().isAfter(form.getDatefin())) {
            error = "La date de debut doit preceder la date de fin";
        }
        if (error == null) {
            form.setNumerocompte(form.getNumerocompte().trim());
            return null;
        }
        redirectAttributes.addFlashAttribute("error", error);
        redirectAttributes.addFlashAttribute("releveExportForm", form);
        return "redirect:" + formUrl;
    }

    private ResponseEntity<byte[]> excelResponse(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(bytes);
    }
}
