package com.laposte.adminmobile.web;

import com.laposte.adminmobile.entities.Support;
import com.laposte.adminmobile.service.SupportService;
import jakarta.validation.Valid;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Port of {@code creationsupport.xhtml} / {@code SupportCtrl}. Reachable only under
 * /parametres/**, which {@code SecurityConfig} already restricts to ROLE_ADMINISTRATEUR.
 */
@Controller
public class SupportController {

    /** Same e-mail regex as the legacy {@code f:validateRegex} on the "E-Mail" field. */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[\\w.-]*[a-zA-Z0-9_]@[\\w.-]*[a-zA-Z0-9]\\.[a-zA-Z][a-zA-Z.]*[a-zA-Z]");

    private final SupportService supportService;

    @Autowired
    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping("/parametres/creation-support")
    public String form(Model model) {
        model.addAttribute("support", new Support());
        model.addAttribute("listSupport", supportService.allSupport());
        return "support/creation";
    }

    /** Port of {@code SupportCtrl#enregSupport()}. */
    @PostMapping("/parametres/creation-support")
    public String create(@Valid @ModelAttribute("support") Support support,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        requireNotBlank(bindingResult, support.getCodeutilisateur(), "codeutilisateur", "Code utilisateur obligatoire");
        requireNotBlank(bindingResult, support.getNomsupport(), "nomsupport", "Nom support obligatoire");
        requireNotBlank(bindingResult, support.getPrenomsupport(), "prenomsupport", "Prenom support obligatoire");
        requireNotBlank(bindingResult, support.getTelephonsupport(), "telephonsupport", "Telephone Support obligatoire");
        if (support.getMailsupport() == null || support.getMailsupport().isBlank()) {
            bindingResult.rejectValue("mailsupport", "required", "Mail Support obligatoire");
        } else if (!EMAIL_PATTERN.matcher(support.getMailsupport()).matches()) {
            bindingResult.rejectValue("mailsupport", "invalid", "Ecrivez un mail valide ex: laposte@laposte.tg");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("listSupport", supportService.allSupport());
            return "support/creation";
        }

        if (!supportService.getSupportenreg(support.getTelephonsupport()).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Ce support existe deja!");
        } else {
            supportService.create(support);
            redirectAttributes.addFlashAttribute("success", "Support cree avec succes");
        }
        return "redirect:/parametres/creation-support";
    }

    private void requireNotBlank(BindingResult bindingResult, String value, String field, String message) {
        if (value == null || value.isBlank()) {
            bindingResult.rejectValue(field, "required", message);
        }
    }
}
