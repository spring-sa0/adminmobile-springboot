package com.laposte.adminmobile.web;

import com.laposte.adminmobile.service.CashpowerService;
import com.laposte.adminmobile.service.CashpowerService.OperationCashpower;
import com.laposte.adminmobile.service.CashpowerService.ResultatAnnulation;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Portage du formulaire Oracle Forms {@code annul_op.fmb} ("ANNULATION DES OPERATIONS") :
 * annulation d'une operation CashPower (achat d'electricite prepayee).
 * <p>
 * Ecart volontaire avec le formulaire : celui-ci laissait le bouton ANNULER actif meme quand
 * l'operation n'etait pas trouvee dans SPT.CASHPOWER ; or la procedure annule n'importe quelle
 * operation presente dans SPT.MOUVEMENT. Ici, l'annulation n'est possible que pour une operation
 * CashPower existante (verifie a nouveau cote serveur au moment de l'annulation).
 * <p>
 * Reserve aux administrateurs (regle /annulation-op/** de SecurityConfig). Chaque annulation est
 * tracee dans audit.log (AuditInterceptor) et ses ecritures portent l'identifiant de l'agent.
 */
@Controller
@RequestMapping("/annulation-op/cashpower")
public class AnnulationCashpowerController {

    private static final String VUE = "annulation-op/cashpower";
    private static final String RETOUR = "redirect:/annulation-op/cashpower";

    private final CashpowerService cashpowerService;

    public AnnulationCashpowerController(CashpowerService cashpowerService) {
        this.cashpowerService = cashpowerService;
    }

    @GetMapping
    public String rechercher(@RequestParam(required = false) String numop, Model model) {
        model.addAttribute("numop", numop);
        if (numop == null || numop.isBlank()) {
            return VUE;
        }
        Long numero = numero(numop);
        if (numero == null) {
            model.addAttribute("error", "Numero d'operation invalide : chiffres uniquement.");
            return VUE;
        }
        List<OperationCashpower> operations = cashpowerService.rechercher(numero);
        model.addAttribute("results", operations);
        if (operations.isEmpty()) {
            model.addAttribute("error", "Aucune operation CashPower trouvee pour le numero " + numero + ".");
        } else {
            model.addAttribute("annulationExistante", cashpowerService.annulationExistante(numero));
        }
        return VUE;
    }

    @PostMapping("/annuler")
    public String annuler(@RequestParam String numop, Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        Long numero = numero(numop);
        if (numero == null) {
            redirectAttributes.addFlashAttribute("error", "Numero d'operation invalide : chiffres uniquement.");
            return RETOUR;
        }
        redirectAttributes.addAttribute("numop", numero);
        if (cashpowerService.rechercher(numero).isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "Annulation refusee : l'operation " + numero + " n'est pas une operation CashPower.");
            return RETOUR;
        }

        ResultatAnnulation resultat = cashpowerService.annuler(numero, authentication != null ? authentication.getName() : null);
        // Meme affichage que le formulaire : "code - message" de la procedure.
        String message = resultat.code() + " - " + (resultat.message() == null ? "" : resultat.message());
        redirectAttributes.addFlashAttribute(resultat.reussi() ? "success" : "error", message);
        return RETOUR;
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
