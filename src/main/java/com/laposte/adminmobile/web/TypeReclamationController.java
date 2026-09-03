package com.laposte.adminmobile.web;

import com.laposte.adminmobile.entities.TypeReclammation;
import com.laposte.adminmobile.service.TypeReclammationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Port of {@code typerecla.xhtml} / {@code TypereclaCtrl}: CRUD (create only, as in the legacy
 * screen) for complaint types. Conceptually lives under "Parametres" but its URL is
 * /reclamations/types, outside the /parametres/** prefix that {@code SecurityConfig} already
 * restricts to ROLE_ADMINISTRATEUR - hence the explicit {@code @PreAuthorize} below (per task
 * brief; requires {@code @EnableMethodSecurity}, added on {@code SecurityConfig}).
 */
@Controller
public class TypeReclamationController {

    private final TypeReclammationService typeReclammationService;

    @Autowired
    public TypeReclamationController(TypeReclammationService typeReclammationService) {
        this.typeReclammationService = typeReclammationService;
    }

    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @GetMapping("/reclamations/types")
    public String form(Model model) {
        model.addAttribute("typeReclammation", new TypeReclammation());
        model.addAttribute("listTypes", typeReclammationService.allTypeRecla());
        return "reclamations/types";
    }

    /** Port of {@code TypereclaCtrl#enregTypeRecla()}. */
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @PostMapping("/reclamations/types")
    public String create(@Valid @ModelAttribute("typeReclammation") TypeReclammation typeReclammation,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (typeReclammation.getLibellerecla() == null || typeReclammation.getLibellerecla().isBlank()) {
            bindingResult.rejectValue("libellerecla", "required", "Libelle obligatoire");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("listTypes", typeReclammationService.allTypeRecla());
            return "reclamations/types";
        }
        if (!typeReclammationService.getTypeenreg(typeReclammation.getLibellerecla()).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Ce Type existe deja!");
        } else {
            typeReclammationService.create(typeReclammation);
            redirectAttributes.addFlashAttribute("success", "Type Reclamation cree avec succes");
        }
        return "redirect:/reclamations/types";
    }
}
