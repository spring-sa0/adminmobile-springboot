package com.laposte.adminmobile.web;

import com.laposte.adminmobile.service.OperationsEnCours;
import com.laposte.adminmobile.service.RecuElectroniqueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Port of {@code annulop.xhtml} and {@code restaureannul.xhtml}, backed by
 * {@code OpCtrl#afficherope/annulerop/afficheresto/restaureop}. The stored-procedure calls
 * themselves are already ported on {@link RecuElectroniqueService#annulopeco} /
 * {@link RecuElectroniqueService#restaurerop} (see that class); this controller just wires the
 * search-then-confirm screens around them. Routes are gated to ROLE_ADMINISTRATEUR by
 * {@code SecurityConfig}'s existing {@code /annulation-op/**} rule.
 * <p>
 * Protections ajoutees apres le double clic du 21/10/2026 (operation 103952083 laissee a moitie
 * annulee) : les procedures durent 30 a 60 s, ne renvoient aucun resultat et ne supportent pas
 * deux executions simultanees. Donc :
 * <ul>
 *   <li>une operation deja annulee (contre-ecriture existante) n'est jamais re-annulee ;</li>
 *   <li>une annulation / restauration deja en cours pour le meme numero est refusee ;</li>
 *   <li>le resultat affiche est verifie en base apres l'appel (contre-ecriture creee, ou supprimee
 *       pour une restauration) au lieu d'un "OK" systematique.</li>
 * </ul>
 */
@Controller
@RequestMapping("/annulation-op")
public class AnnulationOpController {

    private static final Logger log = LoggerFactory.getLogger(AnnulationOpController.class);

    static final String ANNULATION = "ANNULATION_ECOCCP";
    static final String RESTAURATION = "RESTAURATION_ECOCCP";

    private final RecuElectroniqueService recuElectroniqueService;
    private final OperationsEnCours operationsEnCours;

    @Autowired
    public AnnulationOpController(RecuElectroniqueService recuElectroniqueService, OperationsEnCours operationsEnCours) {
        this.recuElectroniqueService = recuElectroniqueService;
        this.operationsEnCours = operationsEnCours;
    }

    /** Port of OpCtrl#afficherope(): looks up the operation to annul by its numero_operation. */
    @GetMapping("/annuler")
    public String annulerForm(@RequestParam(required = false) String numop, Model model) {
        model.addAttribute("numop", numop);
        Long numero = numero(numop);
        if (numop != null && !numop.isBlank()) {
            if (numero == null) {
                model.addAttribute("error", "Numero d'operation invalide.");
                return "annulation-op/annuler";
            }
            var results = recuElectroniqueService.recupop(numero.toString());
            model.addAttribute("results", results);
            if (results.isEmpty()) {
                model.addAttribute("error", "Cette transaction n'existe pas! Veuillez verifier le numero op.");
            } else {
                // Deja annulee : on le dit et on ne propose pas le bouton (une seconde annulation
                // extournerait une seconde fois le montant en comptabilite).
                model.addAttribute("contreEcriture", recuElectroniqueService.contreEcritureDe(numero));
            }
        }
        return "annulation-op/annuler";
    }

    /** Port of OpCtrl#annulerop(): calls the annul_op_ecoccp stored procedure. */
    @PostMapping("/annuler")
    public String annuler(@RequestParam String numop,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        Long numero = numero(numop);
        if (numero == null) {
            redirectAttributes.addFlashAttribute("error", "Numero d'operation invalide.");
            redirectAttributes.addAttribute("numop", numop);
            return "redirect:/annulation-op/annuler";
        }
        redirectAttributes.addAttribute("numop", numero);

        Long dejaFaite = recuElectroniqueService.contreEcritureDe(numero);
        if (dejaFaite != null) {
            redirectAttributes.addFlashAttribute("error", "Operation " + numero + " deja annulee (contre-ecriture n° "
                    + dejaFaite + "). Aucune nouvelle annulation n'a ete faite.");
            return "redirect:/annulation-op/annuler";
        }
        if (!operationsEnCours.demarrer(ANNULATION, numero)) {
            redirectAttributes.addFlashAttribute("error", "Une annulation de l'operation " + numero
                    + " est deja en cours (elle peut prendre jusqu'a une minute). Patientez puis rechargez la page.");
            return "redirect:/annulation-op/annuler";
        }
        try {
            String login = authentication != null ? authentication.getName() : null;
            recuElectroniqueService.annulopeco(numero.doubleValue(), login);
        } catch (RuntimeException e) {
            log.error("Echec de la procedure d'annulation pour l'operation {}", numero, e);
            redirectAttributes.addFlashAttribute("error", "La procedure d'annulation a echoue pour l'operation " + numero
                    + ". Detail dans la supervision (erreurs).");
            return "redirect:/annulation-op/annuler";
        } finally {
            operationsEnCours.terminer(ANNULATION, numero);
        }

        // La procedure ne renvoie rien : on verifie en base qu'elle a bien cree la contre-ecriture.
        Long contreEcriture = recuElectroniqueService.contreEcritureDe(numero);
        if (contreEcriture != null) {
            redirectAttributes.addFlashAttribute("success", "Operation " + numero + " annulee. Contre-ecriture n° "
                    + contreEcriture + ".");
        } else {
            redirectAttributes.addFlashAttribute("error", "L'operation " + numero + " n'a pas ete annulee : la procedure "
                    + "n'a cree aucune contre-ecriture (operation non annulable, ex. code d'operation non EcoCCP).");
        }
        return "redirect:/annulation-op/annuler";
    }

    /** Port of OpCtrl#afficheresto(). */
    @GetMapping("/restaurer")
    public String restaurerForm(@RequestParam(required = false) String numop, Model model) {
        model.addAttribute("numop", numop);
        Long numero = numero(numop);
        if (numop != null && !numop.isBlank()) {
            if (numero == null) {
                model.addAttribute("error", "Numero d'operation invalide.");
                return "annulation-op/restaurer";
            }
            var results = recuElectroniqueService.recupop(numero.toString());
            model.addAttribute("results", results);
            if (results.isEmpty()) {
                model.addAttribute("error", "Cette transaction n'existe pas! Veuillez verifier le numero op.");
            } else {
                Long operationAnnulee = recuElectroniqueService.operationAnnuleePar(numero);
                model.addAttribute("operationAnnulee", operationAnnulee);
                if (operationAnnulee == null) {
                    model.addAttribute("error", "Le numero " + numero + " n'est pas une contre-ecriture d'annulation : "
                            + "saisir le numero de la contre-ecriture (operation \"ANNUL ...\"), pas celui de l'operation d'origine.");
                }
            }
        }
        return "annulation-op/restaurer";
    }

    /** Port of OpCtrl#restaureop(): calls the restaurer_op_ecoccp stored procedure. */
    @PostMapping("/restaurer")
    public String restaurer(@RequestParam String numop,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        Long numero = numero(numop);
        if (numero == null) {
            redirectAttributes.addFlashAttribute("error", "Numero d'operation invalide.");
            redirectAttributes.addAttribute("numop", numop);
            return "redirect:/annulation-op/restaurer";
        }
        redirectAttributes.addAttribute("numop", numero);

        Long operationAnnulee = recuElectroniqueService.operationAnnuleePar(numero);
        if (operationAnnulee == null) {
            redirectAttributes.addFlashAttribute("error", "Le numero " + numero + " n'est pas (ou plus) une contre-ecriture "
                    + "d'annulation : rien a restaurer.");
            return "redirect:/annulation-op/restaurer";
        }
        if (recuElectroniqueService.existe(operationAnnulee)) {
            // Etat incoherent (ex. 103952083 apres le double clic du 21/10/2026) : la procedure
            // reinsererait l'operation d'origine une fois de plus.
            redirectAttributes.addFlashAttribute("error", "Restauration refusee : l'operation d'origine " + operationAnnulee
                    + " est deja presente dans les operations alors qu'elle est annulee par " + numero
                    + ". Etat incoherent a faire corriger par le DBA avant toute restauration.");
            return "redirect:/annulation-op/restaurer";
        }
        if (!operationsEnCours.demarrer(RESTAURATION, numero)) {
            redirectAttributes.addFlashAttribute("error", "Une restauration de la contre-ecriture " + numero
                    + " est deja en cours. Patientez puis rechargez la page.");
            return "redirect:/annulation-op/restaurer";
        }
        try {
            String login = authentication != null ? authentication.getName() : null;
            recuElectroniqueService.restaurerop(numero.doubleValue(), login);
        } catch (RuntimeException e) {
            log.error("Echec de la procedure de restauration pour la contre-ecriture {}", numero, e);
            redirectAttributes.addFlashAttribute("error", "La procedure de restauration a echoue pour " + numero
                    + ". Detail dans la supervision (erreurs).");
            return "redirect:/annulation-op/restaurer";
        } finally {
            operationsEnCours.terminer(RESTAURATION, numero);
        }

        // La restauration supprime la contre-ecriture et remet l'operation d'origine : on le verifie.
        if (!recuElectroniqueService.existe(numero) && recuElectroniqueService.existe(operationAnnulee)) {
            redirectAttributes.addFlashAttribute("success", "Operation " + operationAnnulee
                    + " restauree (contre-ecriture " + numero + " supprimee).");
        } else {
            redirectAttributes.addFlashAttribute("error", "La restauration de " + numero + " n'a pas abouti : "
                    + "la procedure n'a pas supprime la contre-ecriture ou n'a pas remis l'operation " + operationAnnulee + ".");
        }
        return "redirect:/annulation-op/restaurer";
    }

    private static Long numero(String saisie) {
        try {
            long n = Long.parseLong(saisie.trim());
            return n > 0 ? n : null;
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }
}
