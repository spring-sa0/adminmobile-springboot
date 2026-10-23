package com.laposte.adminmobile.web;

import com.laposte.adminmobile.service.CashpowerService;
import com.laposte.adminmobile.service.OperationsEnCours;
import com.laposte.adminmobile.service.CashpowerService.OperationCashpower;
import com.laposte.adminmobile.service.CashpowerService.ResultatAnnulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Apres l'annulation, l'achat passe au statut {@link CashpowerService#STATUT_REMBOURSE} dans
 * SPT.CASHPOWER (la procedure ne touche pas a cette table). Un bouton met a jour en une fois tous
 * les achats annules auparavant.
 * <p>
 * Reserve aux administrateurs (regle /annulation-op/** de SecurityConfig). Chaque annulation est
 * tracee dans audit.log (AuditInterceptor) et ses ecritures portent l'identifiant de l'agent.
 */
@Controller
@RequestMapping("/annulation-op/cashpower")
public class AnnulationCashpowerController {

    private static final String VUE = "annulation-op/cashpower";
    private static final String RETOUR = "redirect:/annulation-op/cashpower";

    static final String ANNULATION = "ANNULATION_CASHPOWER";
    static final String MISE_A_JOUR_STATUTS = "STATUTS_CASHPOWER";

    private static final Logger log = LoggerFactory.getLogger(AnnulationCashpowerController.class);

    private final CashpowerService cashpowerService;
    private final OperationsEnCours operationsEnCours;

    public AnnulationCashpowerController(CashpowerService cashpowerService, OperationsEnCours operationsEnCours) {
        this.cashpowerService = cashpowerService;
        this.operationsEnCours = operationsEnCours;
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
            Long annulation = cashpowerService.annulationExistante(numero);
            model.addAttribute("annulationExistante", annulation);
            model.addAttribute("statutARattraper", annulation != null && operations.stream()
                    .anyMatch(o -> !CashpowerService.STATUT_REMBOURSE.equals(o.statut())));
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

        // Un double clic lancerait deux appels simultanes, qui passeraient tous deux le controle
        // "deja annulee" de la procedure (verification puis insertion, sans verrou) : un seul a la fois.
        if (!operationsEnCours.demarrer(ANNULATION, numero)) {
            redirectAttributes.addFlashAttribute("error", "Une annulation de l'operation " + numero
                    + " est deja en cours. Patientez puis rechargez la page.");
            return RETOUR;
        }
        ResultatAnnulation resultat;
        try {
            resultat = cashpowerService.annuler(numero, authentication != null ? authentication.getName() : null);
        } finally {
            operationsEnCours.terminer(ANNULATION, numero);
        }
        // Meme affichage que le formulaire : "code - message" de la procedure.
        String message = resultat.code() + " - " + (resultat.message() == null ? "" : resultat.message());
        // 0 : annulee a l'instant ; 2 : deja annulee (par exemple par le formulaire Forms, qui ne
        // changeait pas le statut) : dans les deux cas l'achat est rembourse.
        if (resultat.reussi() || resultat.code() == 2) {
            message += statutMisAJour(numero);
        }
        redirectAttributes.addFlashAttribute(resultat.reussi() ? "success" : "error", message);
        return RETOUR;
    }

    /**
     * Rattrapage pour une operation deja annulee dont l'achat n'a pas le statut rembourse (annulations
     * faites avant la version 1.12.0 ou par le formulaire Forms).
     */
    @PostMapping("/statut")
    public String marquerRembourse(@RequestParam String numop, RedirectAttributes redirectAttributes) {
        Long numero = numero(numop);
        if (numero == null) {
            redirectAttributes.addFlashAttribute("error", "Numero d'operation invalide : chiffres uniquement.");
            return RETOUR;
        }
        redirectAttributes.addAttribute("numop", numero);
        if (cashpowerService.annulationExistante(numero) == null) {
            redirectAttributes.addFlashAttribute("error", "Statut non modifie : l'operation " + numero
                    + " n'a pas ete annulee.");
            return RETOUR;
        }
        String message = statutMisAJour(numero);
        redirectAttributes.addFlashAttribute(message.contains("ECHEC") ? "error" : "success",
                "Operation " + numero + " deja annulee." + message);
        return RETOUR;
    }

    /**
     * Mise a jour en une fois de tous les achats deja annules qui n'ont pas le statut rembourse
     * (environ une minute). Sans effet sur la comptabilite ; peut etre relancee sans risque.
     */
    @PostMapping("/statuts")
    public String marquerTousRembourses(RedirectAttributes redirectAttributes) {
        if (!operationsEnCours.demarrer(MISE_A_JOUR_STATUTS, 0)) {
            redirectAttributes.addFlashAttribute("error", "Une mise a jour des statuts est deja en cours. "
                    + "Patientez puis rechargez la page.");
            return RETOUR;
        }
        try {
            int lignes = cashpowerService.marquerTousRembourses();
            redirectAttributes.addFlashAttribute("success", lignes == 0
                    ? "Aucun achat a mettre a jour : tous les achats annules ont deja le statut "
                    + CashpowerService.STATUT_REMBOURSE + "."
                    : lignes + " achat(s) CashPower passe(s) au statut " + CashpowerService.STATUT_REMBOURSE + ".");
        } catch (RuntimeException e) {
            log.error("Mise a jour des statuts CashPower rembourses en echec", e);
            redirectAttributes.addFlashAttribute("error", "ECHEC de la mise a jour des statuts (aucune ligne"
                    + " modifiee) : detail dans Supervision > Etat de sante et erreurs.");
        } finally {
            operationsEnCours.terminer(MISE_A_JOUR_STATUTS, 0);
        }
        return RETOUR;
    }

    /** Passe l'achat au statut rembourse ; l'annulation comptable est faite quoi qu'il arrive ici. */
    private String statutMisAJour(long numero) {
        try {
            int lignes = cashpowerService.marquerRembourse(numero);
            return lignes > 0 ? " Achat passe au statut " + CashpowerService.STATUT_REMBOURSE + "."
                    : " Achat deja au statut " + CashpowerService.STATUT_REMBOURSE + ".";
        } catch (RuntimeException e) {
            log.error("Statut CashPower non mis a jour pour l'operation {} annulee", numero, e);
            return " ECHEC de la mise a jour du statut de l'achat (l'annulation comptable est faite) :"
                    + " utilisez le bouton de mise a jour du statut.";
        }
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
