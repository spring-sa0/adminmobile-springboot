package com.laposte.adminmobile.web;

import com.laposte.adminmobile.security.LegacyDbAuthenticationProvider;
import com.laposte.adminmobile.security.SecurityConfig;
import com.laposte.adminmobile.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.DefaultErrorViewResolver;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import jakarta.servlet.RequestDispatcher;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke test of every screen: renders each GET page (with service mocks returning one populated
 * row per list, so every table body is evaluated) and exercises the POST actions, without any
 * database. Catches broken templates, unknown properties, missing routes and access-rule gaps.
 */
@WebMvcTest
@Import({SecurityConfig.class, ErrorReferenceAttributes.class, PaginationEnBase.class,
        com.laposte.adminmobile.service.OperationsEnCours.class, AllPagesTest.MockServices.class})
class AllPagesTest {

    private static final String P = "date1=2026-01-01&date2=2026-01-31";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsersService usersService;

    @Autowired
    private RecuElectroniqueService recuElectroniqueService;

    @Autowired
    private CashpowerService cashpowerService;

    @Autowired
    private SuggestionsService suggestionsService;

    @Autowired
    private SupportService supportService;

    @Autowired
    private TypeReclammationService typeReclammationService;

    @Autowired
    private com.laposte.adminmobile.supervision.EtatSanteService etatSanteService;

    @Autowired
    private MailService mailService;

    /** Etat de sante type : une alerte critique (mot de passe Oracle expire), le reste OK. */
    @org.junit.jupiter.api.BeforeEach
    void etatDeSanteExemple() {
        var critique = new com.laposte.adminmobile.supervision.EtatSanteService.Indicateur("COMPTE_ORACLE",
                "Compte Oracle de l'application", "EXPIRED(GRACE)",
                com.laposte.adminmobile.supervision.EtatSanteService.Niveau.CRITIQUE,
                "Mot de passe expire", "Faire changer le mot de passe et mettre a jour DB_PASSWORD");
        var ok = new com.laposte.adminmobile.supervision.EtatSanteService.Indicateur("BASE", "Base Oracle", "12 ms",
                com.laposte.adminmobile.supervision.EtatSanteService.Niveau.OK, "Temps de reponse", "");
        Mockito.doReturn(new com.laposte.adminmobile.supervision.EtatSanteService.EtatSante(
                        com.laposte.adminmobile.supervision.EtatSanteService.Niveau.CRITIQUE, new Date(), "1.8.0", new Date(),
                        "2 h 5 min", List.of(critique, ok)))
                .when(etatSanteService).etat();
    }

    @TestConfiguration
    static class MockServices {
        /**
         * @WebMvcTest n'enregistre pas le resolveur qui choisit error/404.html, error/5xx.html...
         * selon le code HTTP (il retomberait toujours sur error.html) : on le declare comme en prod.
         */
        @Bean
        DefaultErrorViewResolver errorViewResolver(ApplicationContext context) {
            return new DefaultErrorViewResolver(context, new WebProperties.Resources());
        }

        @Bean
        static BeanDefinitionRegistryPostProcessor registerMocks() {
            return new BeanDefinitionRegistryPostProcessor() {
                @Override
                public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
                }

                @Override
                public void postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory f) {
                    List<Class<?>> types = List.of(
                            ApplimobileService.class, BankToBankReportService.class, CashpowerService.class, CommissionsReportService.class,
                            com.laposte.adminmobile.supervision.EtatSanteService.class,
                            CompteAppLierService.class, ComptesReportService.class, EposteFabricatedReportService.class,
                            FormatsService.class, FrequencesService.class, JasperReportService.class, MailService.class,
                            MarchandReportService.class, ModifCodesecretService.class, OperationService.class,
                            ProfilService.class, RecapReportService.class, ReclamationsService.class,
                            RecuElectroniqueService.class, ReleveExportService.class, ReleveGridExportService.class,
                            ReleveService.class, SmsService.class, SuggestionsService.class, SupportService.class,
                            TypeReclammationService.class, UsersService.class, LegacyDbAuthenticationProvider.class);
                    for (Class<?> type : types) {
                        // Registered as ready-made singletons so that no @PersistenceContext /
                        // @Autowired processing is applied to the mocks.
                        f.registerSingleton(type.getSimpleName() + "Mock", Mockito.mock(type, new SampleDataAnswer()));
                    }
                }
            };
        }
    }

    /** Toutes les pages GET de l'application, avec et sans criteres de recherche. */
    static Stream<String> pages() {
        return Stream.of(
            "/", "/login",
            "/parametres/modif-profil", "/parametres/modif-profil?telephone=90000000&numeroCompte=1",
            "/parametres/modif-password", "/parametres/modif-password?telephone=90000000",
            "/parametres/modif-password/generate",
            "/parametres/modif-email", "/parametres/modif-email?telephone=90000000",
            "/parametres/activer-desactiver", "/parametres/activer-desactiver?telephone=90000000",
            "/parametres/creation-support",
            "/reclamations", "/reclamations/types", "/reclamations/liste", "/reclamations/liste?" + P + "&etat=TOUT",
            "/reclamations/affectation", "/reclamations/cloture",
            "/suggestions", "/suggestions/liste", "/suggestions/liste?" + P,
            "/consultations/operations", "/consultations/operations?ptelephone=90000000&" + P,
            "/consultations/operations?numcompte=123&" + P,
            "/consultations/operation", "/consultations/operation?numeroop=1", "/consultations/operation?referenceid=x",
            "/operations/liste", "/operations/liste?codeOperation=1&" + P,
            "/operations/par-client", "/operations/par-client?telephone=90000000&" + P,
            "/operations/par-compte", "/operations/par-compte?numerocompte=1&" + P,
            "/comptes/lies", "/comptes/lies?telephone=90000000",
            "/annulation-op/annuler", "/annulation-op/annuler?numop=1",
            "/annulation-op/restaurer", "/annulation-op/restaurer?numop=1",
            "/annulation-op/cashpower", "/annulation-op/cashpower?numop=1", "/annulation-op/cashpower?numop=abc",
            "/annulation-op/annulees", "/annulation-op/annulees?" + P, "/annulation-op/annulees?type=CASHPOWER&" + P,
            "/supervision", "/supervision?gravite=CRITIQUE", "/supervision?resolues=true", "/supervision/catalogue",
            "/etats/transactions/bank-to-wallet", "/etats/transactions/bank-to-wallet?operateur=TMoney&" + P,
            "/etats/transactions/wallet-to-bank", "/etats/transactions/wallet-to-bank?" + P,
            "/etats/transactions/bank-to-bank", "/etats/transactions/bank-to-bank?operateur=FLooz&" + P,
            "/etats/transactions/compte-a-compte", "/etats/transactions/compte-a-compte?" + P,
            "/etats/transactions/retrait", "/etats/transactions/retrait?" + P,
            "/etats/transactions/depot", "/etats/transactions/depot?" + P,
            "/etats/transactions/global", "/etats/transactions/global?" + P,
            "/etats/transactions/ecoccp", "/etats/transactions/ecoccp?" + P,
            "/etats/transactions/ecoccp-annulees", "/etats/transactions/ecoccp-annulees?" + P,
            "/comptes/nombre", "/comptes/nombre?" + P,
            "/comptes/liste-ecoccp", "/comptes/liste-ecoccp?" + P,
            "/comptes/actifs", "/comptes/actifs?" + P,
            "/comptes/solde", "/comptes/solde?" + P,
            "/comptes/liste-solde", "/comptes/liste-solde?" + P,
            "/comptes/negatifs",
            "/eposte/operations", "/eposte/operations?" + P,
            "/eposte/operations-client", "/eposte/operations-client?telephone=90000000&" + P,
            "/eposte/operations-compte", "/eposte/operations-compte?numerocompte=1&" + P,
            "/eposte/comptes-op-effectuees", "/eposte/comptes-op-effectuees?" + P,
            "/eposte/comptes-op-non-effectuees", "/eposte/comptes-op-non-effectuees?" + P,
            "/eposte/souscription", "/eposte/souscription?operateur=Tout&" + P,
            "/eposte/chequiers", "/eposte/chequiers?" + P,
            "/eposte/modif-pwd", "/eposte/modif-pwd?" + P,
            "/eposte/achats-credit", "/eposte/achats-credit?" + P,
            "/eposte/comptes-desactives",
            "/eposte/recap/client", "/eposte/recap/client?" + P,
            "/eposte/recap/compte", "/eposte/recap/compte?" + P,
            "/eposte/recap-global/produit", "/eposte/recap-global/produit?" + P,
            "/eposte/recap-global/souscriptions", "/eposte/recap-global/souscriptions?" + P,
            "/commissions/ecoccp", "/commissions/ecoccp?" + P,
            "/commissions/eposte", "/commissions/eposte?" + P,
            "/releves/liste", "/releves/liste/1/edit", "/releves/parametrage",
            "/releves/excel-afb120", "/releves/excel-togocel", "/releves/intitule?numerocompte=1",
            "/releves/liste/export",
            "/gateway/liste", "/gateway/liste?" + P,
            "/marchand/parametrage", "/marchand/parametrage?telephone=90000000&numerocompte=1",
            "/marchand/activer-desactiver", "/marchand/activer-desactiver?telephone=90000000",
            "/marchand/comptes",
            "/marchand/operations", "/marchand/operations?" + P,
            "/marchand/operations-liste", "/marchand/operations-liste?" + P,
            "/marchand/operations-par-marchand", "/marchand/operations-par-marchand?numerocompte=1&" + P,
            "/marchand/commissions", "/marchand/commissions?" + P
        );
    }

    @ParameterizedTest
    @MethodSource("pages")
    @WithMockUser(
            authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void everyPageRendersForAdministrator(String url) throws Exception {
        MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        assertThat(result.getResponse().getContentAsByteArray()).isNotNull();
    }

    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/comptes/nombre")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_MARCHAND", "MARCHAND"})
    void merchantProfileCannotReachBackOfficeScreens() throws Exception {
        for (String url : List.of("/parametres/modif-password", "/annulation-op/annuler", "/gateway/liste",
                "/reclamations", "/etats/transactions/global", "/consultations/operations", "/comptes/lies",
                "/releves/liste", "/eposte/operations", "/commissions/ecoccp", "/suggestions/liste")) {
            mvc.perform(get(url)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/marchand/comptes")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_CONSULTATION", "PROFIL_SOUSMENU", "MARCHAND"})
    void consultationProfileOnlyReachesReleves() throws Exception {
        mvc.perform(get("/releves/liste")).andExpect(status().isOk());
        mvc.perform(get("/reclamations")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void lastDayOfRangeIsIncluded() throws Exception {
        mvc.perform(get("/suggestions/liste?date1=2026-01-01&date2=2026-01-31")).andExpect(status().isOk());
        org.mockito.ArgumentCaptor<Date> end = org.mockito.ArgumentCaptor.forClass(Date.class);
        verify(suggestionsService, Mockito.atLeastOnce()).findByPeriode(any(), end.capture());
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(end.getValue());
        assertThat(cal.get(java.util.Calendar.DAY_OF_MONTH)).isEqualTo(31);
        assertThat(cal.get(java.util.Calendar.HOUR_OF_DAY)).isEqualTo(23);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void postActionsRedirect() throws Exception {
        mvc.perform(post("/parametres/modif-password").with(csrf())
                .param("telephone", "90000000").param("newpass", "Abc12345").param("motif", "oubli"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/parametres/modif-email").with(csrf())
                .param("telephone", "90000000").param("email", "a@b.tg"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/parametres/modif-profil").with(csrf())
                .param("telephone", "90000000").param("idprofil", "2.00").param("numeroCompte", "123"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/parametres/activer-desactiver").with(csrf())
                .param("telephone", "90000000").param("motif", "x").param("desactiver", ""))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/parametres/activer-desactiver").with(csrf())
                .param("telephone", "90000000").param("motif", "x").param("activer", ""))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/parametres/creation-support").with(csrf())
                .param("codeutilisateur", "u").param("nomsupport", "n").param("prenomsupport", "p")
                .param("telephonsupport", "90000000").param("mailsupport", "a@laposte.tg"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/reclamations").with(csrf())
                .param("telephone", "90000000").param("idtype", "1").param("motif", "m"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/reclamations").with(csrf()).param("telephone", "abc"))
                .andExpect(status().isOk());
        mvc.perform(post("/reclamations/types").with(csrf()).param("libellerecla", "Nouveau"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/reclamations/affectation").with(csrf())
                .param("idreclamation", "1").param("idsupport", "1"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/reclamations/cloture").with(csrf()).param("idreclamation", "1").param("remarques", "ok"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/reclamations/cloture/rendre").with(csrf()).param("idreclamation", "1"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/suggestions").with(csrf()).param("telephone", "90000000").param("motif", "m"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/annulation-op/annuler").with(csrf()).param("numop", "12").param("message", "m"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/annulation-op/restaurer").with(csrf()).param("numop", "12"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/marchand/parametrage").with(csrf()).param("telephone", "90000000").param("numerocompte", "1"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/marchand/activer-desactiver").with(csrf())
                .param("telephone", "90000000").param("motif", "x").param("activer", ""))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/releves/parametrage").with(csrf())
                .param("numerocompte", "1").param("email", "a@b.tg").param("mensuel", "true").param("pdf", "true"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/releves/liste/1/edit").with(csrf()).param("mail", "a@b.tg"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/releves/liste/1/delete").with(csrf())).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void releveExportWithoutCompteDoesNotCallProcedure() throws Exception {
        mvc.perform(post("/releves/excel-togocel/excel").with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/releves/excel-afb120/afb120").with(csrf()).param("numerocompte", "1"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void annulationWithNonNumericNumopDoesNotCallProcedure() throws Exception {
        Mockito.clearInvocations(recuElectroniqueService);
        mvc.perform(post("/annulation-op/annuler").with(csrf()).param("numop", "abc"))
                .andExpect(status().is3xxRedirection());
        verify(recuElectroniqueService, never()).annulopeco(any(), any());
    }

    // ---- Annulation EcoCCP : protections apres l'incident du 21/10/2026 (103952083) ------------

    @Autowired
    private com.laposte.adminmobile.service.OperationsEnCours operationsEnCours;

    @Test
    @WithMockUser(username = "ayekple", authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void uneOperationDejaAnnuleeNEstJamaisReannulee() throws Exception {
        Mockito.doReturn(104077009L).when(recuElectroniqueService).contreEcritureDe(103952083L);

        mvc.perform(get("/annulation-op/annuler?numop=103952083"))
                .andExpect(content().string(containsString("deja annulee (contre-ecriture n° 104077009)")))
                .andExpect(content().string(not(containsString("Annuler Operation</button>"))));

        mvc.perform(post("/annulation-op/annuler").with(csrf()).param("numop", "103952083"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("error", containsString("deja annulee (contre-ecriture n° 104077009)")));
        verify(recuElectroniqueService, never()).annulopeco(eq(103952083d), any());
    }

    @Test
    @WithMockUser(username = "ayekple", authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void unDoubleClicPendantLAnnulationEstRefuse() throws Exception {
        Mockito.doReturn(null).when(recuElectroniqueService).contreEcritureDe(200000001L);
        assertThat(operationsEnCours.demarrer(AnnulationOpController.ANNULATION, 200000001L)).isTrue(); // 1er clic en cours
        try {
            mvc.perform(post("/annulation-op/annuler").with(csrf()).param("numop", "200000001"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                            .attribute("error", containsString("deja en cours")));
            verify(recuElectroniqueService, never()).annulopeco(eq(200000001d), any());
        } finally {
            operationsEnCours.terminer(AnnulationOpController.ANNULATION, 200000001L);
        }
    }

    @Test
    @WithMockUser(username = "ayekple", authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void lAnnulationAfficheLaContreEcritureReellementCreee() throws Exception {
        // Avant l'appel : pas de contre-ecriture ; apres : celle creee par la procedure.
        Mockito.doReturn(null, 300000009L).when(recuElectroniqueService).contreEcritureDe(200000002L);

        mvc.perform(post("/annulation-op/annuler").with(csrf()).param("numop", "200000002"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("success", "Operation 200000002 annulee. Contre-ecriture n° 300000009."));
        verify(recuElectroniqueService).annulopeco(200000002d, "ayekple");
        // Le verrou est libere apres le traitement.
        assertThat(operationsEnCours.demarrer(AnnulationOpController.ANNULATION, 200000002L)).isTrue();
        operationsEnCours.terminer(AnnulationOpController.ANNULATION, 200000002L);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void uneProcedureSansEffetNAffichePlusOk() throws Exception {
        Mockito.doReturn(null).when(recuElectroniqueService).contreEcritureDe(200000003L);

        mvc.perform(post("/annulation-op/annuler").with(csrf()).param("numop", "200000003"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("error", containsString("n'a pas ete annulee")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("success", org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void laRestaurationEstControlee() throws Exception {
        // Numero qui n'est pas une contre-ecriture : refus.
        Mockito.doReturn(null).when(recuElectroniqueService).operationAnnuleePar(200000004L);
        mvc.perform(post("/annulation-op/restaurer").with(csrf()).param("numop", "200000004"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("error", containsString("n'est pas (ou plus) une contre-ecriture")));

        // Etat incoherent (operation d'origine encore presente, cas de 103952083) : refus.
        Mockito.doReturn(103952083L).when(recuElectroniqueService).operationAnnuleePar(104077009L);
        Mockito.doReturn(true).when(recuElectroniqueService).existe(103952083L);
        mvc.perform(post("/annulation-op/restaurer").with(csrf()).param("numop", "104077009"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("error", containsString("Etat incoherent")));
        verify(recuElectroniqueService, never()).restaurerop(eq(104077009d), any());

        // Cas normal : contre-ecriture supprimee et operation remise.
        Mockito.doReturn(200000005L).when(recuElectroniqueService).operationAnnuleePar(300000010L);
        Mockito.doReturn(false, true).when(recuElectroniqueService).existe(200000005L);
        Mockito.doReturn(false).when(recuElectroniqueService).existe(300000010L);
        mvc.perform(post("/annulation-op/restaurer").with(csrf()).param("numop", "300000010"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("success", "Operation 200000005 restauree (contre-ecriture 300000010 supprimee)."));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void unDoubleClicSurLAnnulationCashpowerEstRefuse() throws Exception {
        Mockito.doReturn(List.of(achatCashpower(200000006L))).when(cashpowerService).rechercher(200000006L);
        assertThat(operationsEnCours.demarrer(AnnulationCashpowerController.ANNULATION, 200000006L)).isTrue();
        try {
            mvc.perform(post("/annulation-op/cashpower/annuler").with(csrf()).param("numop", "200000006"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                            .attribute("error", containsString("deja en cours")));
            verify(cashpowerService, never()).annuler(eq(200000006L), any());
        } finally {
            operationsEnCours.terminer(AnnulationCashpowerController.ANNULATION, 200000006L);
        }
    }

    /** Le bouton est desactive des l'envoi, pour qu'un second clic ne parte pas. */
    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void leBoutonDAnnulationSeDesactiveALEnvoi() throws Exception {
        Mockito.doReturn(null).when(recuElectroniqueService).contreEcritureDe(200000007L);
        mvc.perform(get("/annulation-op/annuler?numop=200000007"))
                .andExpect(content().string(containsString("b.disabled = true")))
                .andExpect(content().string(containsString("Annulation en cours, patientez")));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void creationFormsIgnoreInjectedIds() throws Exception {
        Mockito.when(supportService.getSupportenreg(anyString())).thenReturn(List.of());
        mvc.perform(post("/parametres/creation-support").with(csrf()).param("idsupport", "5")
                .param("codeutilisateur", "u").param("nomsupport", "n").param("prenomsupport", "p")
                .param("telephonsupport", "91234567").param("mailsupport", "a@laposte.tg"))
                .andExpect(status().is3xxRedirection());
        verify(supportService).create(Mockito.argThat(s -> s.getIdsupport() == null));

        Mockito.when(typeReclammationService.getTypeenreg(anyString())).thenReturn(List.of());
        mvc.perform(post("/reclamations/types").with(csrf()).param("idtype", "5").param("libellerecla", "Autre"))
                .andExpect(status().is3xxRedirection());
        verify(typeReclammationService).create(Mockito.argThat(t -> t.getIdtype() == null));
    }

    @Test
    void securityHeadersAreSent() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "same-origin"));
    }

    @Test
    void serverErrorPageShowsReferenceButNoTechnicalDetail() throws Exception {
        mvc.perform(get("/error").accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/consultations/operations")
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION,
                                new IllegalStateException("ORA-00904: \"RE1_0\".\"MOTIF_ANNULATION\" select re1_0.numero_operation")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Une erreur est survenue")))
                .andExpect(content().string(matchesPattern("(?s).*reference\\s*<strong>[0-9A-F]{8}</strong>.*")))
                .andExpect(content().string(not(containsString("ORA-"))))
                .andExpect(content().string(not(containsString("select"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void notFoundAndForbiddenHaveDedicatedPages() throws Exception {
        mvc.perform(get("/error").accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Page introuvable")))
                .andExpect(content().string(not(containsString("class=\"err-ref\""))));
        mvc.perform(get("/error").accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 403))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Acces refuse")));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void longResultListsArePaginated() throws Exception {
        List<com.laposte.adminmobile.entities.RecuElectronique> ops = new java.util.ArrayList<>();
        for (long i = 1; i <= 250; i++) {
            ops.add(new com.laposte.adminmobile.entities.RecuElectronique(i));
        }
        Mockito.doAnswer(SampleDataAnswer.pageDe(ops)).when(recuElectroniqueService).recupListopParPeriode(any(), any(), any());

        MvcResult result = mvc.perform(get("/etats/transactions/ecoccp?" + P + "&page=3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lignes 201 a 250 sur 250")))
                .andReturn();
        // Pagination faite par la base : seule la page 3 (100 lignes a partir de la 201e) est demandee.
        verify(recuElectroniqueService).recupListopParPeriode(any(), any(),
                eq(org.springframework.data.domain.PageRequest.of(2, 100)));
        String html = result.getResponse().getContentAsString();
        // 50 lignes de donnees sur la derniere page, et les liens gardent les filtres de la recherche.
        assertThat(html.split("<td>2[0-5][0-9]</td>", -1).length - 1).isEqualTo(50);
        assertThat(html).contains("date1=2026-01-01&amp;date2=2026-01-31&amp;page=2");

        mvc.perform(get("/etats/transactions/ecoccp?" + P))
                .andExpect(content().string(containsString("Lignes 1 a 100 sur 250")));
    }

    /**
     * Chaque page de l'application peut etre demandee en Excel et en PDF : soit elle contient une
     * liste et renvoie le fichier, soit elle n'en contient pas et renvoie la page HTML. Jamais d'erreur.
     */
    @ParameterizedTest
    @MethodSource("pages")
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void everyPageCanBeExported(String url) throws Exception {
        String typePage = mvc.perform(get(url)).andReturn().getResponse().getContentType();
        if (typePage == null || !typePage.startsWith("text/html")) {
            return; // pas une page (telechargement, texte AJAX...) : rien a exporter
        }
        for (String format : List.of("xlsx", "pdf")) {
            String type = mvc.perform(get(url + (url.contains("?") ? "&" : "?") + "export=" + format))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentType();
            assertThat(type).as(url + " en " + format)
                    .satisfiesAnyOf(t -> assertThat(t).startsWith("text"), t -> assertThat(t).contains(
                            format.equals("pdf") ? "application/pdf" : "spreadsheetml"));
        }
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void listExportContainsWholeListNotJustTheDisplayedPage() throws Exception {
        List<com.laposte.adminmobile.entities.RecuElectronique> ops = new java.util.ArrayList<>();
        for (long i = 1; i <= 250; i++) {
            com.laposte.adminmobile.entities.RecuElectronique op = new com.laposte.adminmobile.entities.RecuElectronique(i);
            op.setNumeroCompte(6310319794001000L);
            op.setMontant(1500L * i);
            ops.add(op);
        }
        Mockito.doAnswer(SampleDataAnswer.pageDe(ops)).when(recuElectroniqueService).recupListopParPeriode(any(), any(), any());

        MvcResult xlsx = mvc.perform(get("/etats/transactions/ecoccp?" + P + "&page=2&export=xlsx"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("liste-des-operations-ecoccp-")))
                .andExpect(header().string("Content-Disposition", containsString(".xlsx")))
                .andReturn();
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook classeur = new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                new java.io.ByteArrayInputStream(xlsx.getResponse().getContentAsByteArray()))) {
            org.apache.poi.ss.usermodel.Sheet feuille = classeur.getSheetAt(0);
            assertThat(feuille.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Liste des Operations EcoCCP");
            assertThat(feuille.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Date debut : 2026-01-01");
            int entete = 5;
            assertThat(feuille.getRow(entete).getCell(0).getStringCellValue()).isEqualTo("Telephone");
            assertThat(feuille.getLastRowNum() - entete).as("toutes les lignes, pas la page 2").isEqualTo(250);
            // Montant : nombre ; numero de compte (16 chiffres) : texte, sans perte de precision.
            assertThat(feuille.getRow(entete + 1).getCell(2).getNumericCellValue()).isEqualTo(1500d);
            assertThat(feuille.getRow(entete + 1).getCell(1).getStringCellValue()).isEqualTo("6310319794001000");
        }

        // Export Excel : toute la liste, bornee au plafond + 1 ligne (pour detecter le depassement).
        verify(recuElectroniqueService).recupListopParPeriode(any(), any(),
                eq(org.springframework.data.domain.PageRequest.of(0, 50_001)));

        MvcResult pdf = mvc.perform(get("/etats/transactions/ecoccp?" + P + "&export=pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn();
        assertThat(new String(pdf.getResponse().getContentAsByteArray(), 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
                .isEqualTo("%PDF-");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void tooLongListIsNotExportedButExplained() throws Exception {
        List<com.laposte.adminmobile.entities.RecuElectronique> ops = new java.util.ArrayList<>();
        for (long i = 1; i <= 5001; i++) {
            ops.add(new com.laposte.adminmobile.entities.RecuElectronique(i));
        }
        Mockito.doAnswer(SampleDataAnswer.pageDe(ops)).when(recuElectroniqueService).recupListopParPeriode(any(), any(), any());

        mvc.perform(get("/etats/transactions/ecoccp?" + P + "&export=pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Export PDF limite a 5000 lignes (5001 trouvees)")))
                .andExpect(content().string(containsString("Lignes 1 a 100 sur 5")));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void actionColumnsAreNotExported() throws Exception {
        MvcResult xlsx = mvc.perform(get("/releves/liste?export=xlsx")).andExpect(status().isOk()).andReturn();
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook classeur = new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                new java.io.ByteArrayInputStream(xlsx.getResponse().getContentAsByteArray()))) {
            org.apache.poi.ss.usermodel.Row entete = classeur.getSheetAt(0).getRow(3);
            List<String> colonnes = new java.util.ArrayList<>();
            entete.forEach(c -> colonnes.add(c.getStringCellValue()));
            assertThat(colonnes).containsExactly("Numero Compte", "Intitule Compte", "Mail", "Frequence", "Format");
        }
    }

    // ---- Annulation CashPower (portage de annul_op.fmb) ----------------------------------------

    private static CashpowerService.OperationCashpower achatCashpower(long numero) {
        return new CashpowerService.OperationCashpower(numero, "KOFFI AMA 01234567890", new java.math.BigDecimal("5000"),
                new Date(), "22890123456", "6310319794001000", "01234567890", "SUCCESS", "R-1", "12.5");
    }

    @Test
    @WithMockUser(username = "agent1", authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void cashpowerOperationIsShownThenCancelledWithProcedureMessage() throws Exception {
        Mockito.doReturn(List.of(achatCashpower(103527700L))).when(cashpowerService).rechercher(103527700L);
        Mockito.doReturn(null).when(cashpowerService).annulationExistante(103527700L);
        Mockito.doReturn(new CashpowerService.ResultatAnnulation(0, "OK - Annulation effectuee avec succes. Numero d'annulation : 103600001"))
                .when(cashpowerService).annuler(103527700L, "agent1");

        mvc.perform(get("/annulation-op/cashpower?numop=103527700"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("KOFFI AMA 01234567890")))
                .andExpect(content().string(containsString("Annuler l'operation")));

        mvc.perform(post("/annulation-op/cashpower/annuler").with(csrf()).param("numop", " 103527700 "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/annulation-op/cashpower?numop=103527700"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("success", "0 - OK - Annulation effectuee avec succes. Numero d'annulation : 103600001"));
        verify(cashpowerService).annuler(103527700L, "agent1");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void procedureRefusalIsShownAsError() throws Exception {
        Mockito.doReturn(List.of(achatCashpower(103527701L))).when(cashpowerService).rechercher(103527701L);
        Mockito.doReturn(new CashpowerService.ResultatAnnulation(1, "NOK - Aucune ecriture trouvee pour l'operation 103527701"))
                .when(cashpowerService).annuler(Mockito.eq(103527701L), any());

        mvc.perform(post("/annulation-op/cashpower/annuler").with(csrf()).param("numop", "103527701"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("error", "1 - NOK - Aucune ecriture trouvee pour l'operation 103527701"));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void onlyCashpowerOperationsCanBeCancelled() throws Exception {
        Mockito.doReturn(List.of()).when(cashpowerService).rechercher(999L);

        mvc.perform(get("/annulation-op/cashpower?numop=999"))
                .andExpect(content().string(containsString("Aucune operation CashPower trouvee")))
                .andExpect(content().string(not(containsString("Annuler l'operation"))));
        mvc.perform(post("/annulation-op/cashpower/annuler").with(csrf()).param("numop", "999"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("error", containsString("n'est pas une operation CashPower")));
        mvc.perform(post("/annulation-op/cashpower/annuler").with(csrf()).param("numop", "12a"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("error", containsString("Numero d'operation invalide")));
        verify(cashpowerService, never()).annuler(Mockito.eq(999L), any());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void alreadyCancelledOperationHasNoCancelButton() throws Exception {
        Mockito.doReturn(List.of(achatCashpower(103527702L))).when(cashpowerService).rechercher(103527702L);
        Mockito.doReturn(103600002L).when(cashpowerService).annulationExistante(103527702L);

        mvc.perform(get("/annulation-op/cashpower?numop=103527702"))
                .andExpect(content().string(containsString("Cette operation est deja annulee. Numero d&#39;annulation : 103600002")))
                .andExpect(content().string(not(containsString("Annuler l'operation"))));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_UTILISATEUR", "PROFIL_USER", "MARCHAND"})
    void onlyAdministratorsCanCancelCashpowerOperations() throws Exception {
        mvc.perform(get("/annulation-op/cashpower")).andExpect(status().isForbidden());
        mvc.perform(post("/annulation-op/cashpower/annuler").with(csrf()).param("numop", "1"))
                .andExpect(status().isForbidden());
        verify(cashpowerService, never()).annuler(Mockito.eq(1L), any());
    }

    // ---- Supervision -------------------------------------------------------------------------

    /**
     * Parcours complet : une vraie erreur 500 (colonne Oracle inexistante) apparait dans la
     * supervision avec son diagnostic, ses etapes de resolution et la reference vue par l'utilisateur,
     * puis est marquee resolue.
     */
    @Test
    @WithMockUser(username = "admin1", authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void uneErreurApparaitDansLaSupervisionAvecSaSolution() throws Exception {
        MvcResult page500 = mvc.perform(get("/error").accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/consultations/operations")
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("acces base",
                                new java.sql.SQLException("ORA-00904: \"RE1_0\".\"COLONNE_TEST_SUPERVISION\" : identificateur non valide"))))
                .andReturn();
        String reference = page500.getResponse().getContentAsString().replaceAll("(?s).*<strong>([0-9A-F]{8})</strong>.*", "$1");

        com.laposte.adminmobile.supervision.GroupeErreurs groupe = com.laposte.adminmobile.supervision.JournalErreurs.instance()
                .groupes().stream()
                .filter(g -> g.getDerniereOccurrence() != null && reference.equals(g.getDerniereOccurrence().reference()))
                .findFirst().orElseThrow();
        assertThat(groupe.getDiagnostic().code()).isEqualTo("SCHEMA_COLONNE_INEXISTANTE");

        mvc.perform(get("/supervision"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Probleme critique en cours")))
                .andExpect(content().string(containsString("Faire changer le mot de passe et mettre a jour DB_PASSWORD")))
                .andExpect(content().string(containsString("Colonne inexistante en base")));

        mvc.perform(get("/supervision/erreurs/" + groupe.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Comment resoudre")))
                .andExpect(content().string(containsString("all_tab_columns")))
                .andExpect(content().string(containsString(reference)))
                .andExpect(content().string(containsString("/consultations/operations")))
                .andExpect(content().string(containsString("COLONNE_TEST_SUPERVISION")));

        mvc.perform(post("/supervision/erreurs/" + groupe.getId() + "/resolu").with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(groupe.isResolu()).isTrue();
        assertThat(groupe.getResoluPar()).isEqualTo("admin1");
    }

    /** Sans destinataire configure (cas des tests), le bouton explique quoi renseigner. */
    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void leTestDEnvoiSansDestinataireExpliqueQuoiRenseigner() throws Exception {
        // Simulation partagee : d'autres tests (notifications de reclamations...) l'ont deja appelee.
        Mockito.clearInvocations(mailService);
        mvc.perform(post("/supervision/test-mail").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("detailEchecMail", containsString("SUPERVISION_ALERTES_EMAILS")));
        Mockito.verify(mailService, never()).sendMessage(anyString(), anyString(), anyString());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_UTILISATEUR", "PROFIL_USER", "MARCHAND"})
    void seulsLesAdministrateursAccedentALaSupervision() throws Exception {
        mvc.perform(get("/supervision")).andExpect(status().isForbidden());
        mvc.perform(get("/supervision/catalogue")).andExpect(status().isForbidden());
        mvc.perform(post("/supervision/erreurs/1/resolu").with(csrf())).andExpect(status().isForbidden());
    }

    /** L'export suit les regles d'acces de la page : pas de fichier sans les droits sur l'ecran. */
    @Test
    void exportRespectsPageAccessRules() throws Exception {
        mvc.perform(get("/etats/transactions/ecoccp?" + P + "&export=xlsx"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_MARCHAND", "MARCHAND"})
    void merchantCannotExportBackOfficeLists() throws Exception {
        mvc.perform(get("/etats/transactions/ecoccp?" + P + "&export=pdf")).andExpect(status().isForbidden());
    }

    /** Toute liste affichee (tableau data-table) doit proposer l'export Excel / PDF. */
    @Test
    void everyListTemplateOffersExport() throws Exception {
        org.springframework.core.io.Resource[] templates = new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
                .getResources("classpath:templates/**/*.html");
        int listes = 0;
        for (org.springframework.core.io.Resource template : templates) {
            String html = template.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            if (html.contains("<table class=\"data-table\"")) {
                listes++;
                assertThat(html).as(template.getFilename()).contains("fragments/export :: boutons");
            }
        }
        assertThat(listes).isEqualTo(63);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void searchPeriodIsLimited() throws Exception {
        mvc.perform(get("/etats/transactions/global?date1=2025-01-01&date2=2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Periode trop longue : 92 jours maximum")));
        mvc.perform(get("/etats/transactions/global?date1=2026-02-01&date2=2026-01-01"))
                .andExpect(content().string(containsString("La date de debut doit preceder la date de fin")));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void phoneNumbersAreNormalizedAndValidated() throws Exception {
        mvc.perform(get("/operations/par-client").param("telephone", "+228 90 12 34 56")
                        .param("date1", "2026-01-01").param("date2", "2026-01-31"))
                .andExpect(status().isOk());
        verify(recuElectroniqueService).recupListopByTelephone(eq("22890123456"), any(), any());

        // Parametres dans l'URL, comme les envoie un formulaire GET du navigateur.
        mvc.perform(get("/operations/par-client?telephone={t}&date1=2026-01-01&date2=2026-01-31", "12ab"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/operations/par-client?date1=2026-01-01&date2=2026-01-31"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("error", containsString("8 chiffres")));
    }

    @Test
    void lockedLoginShowsDedicatedMessage() throws Exception {
        mvc.perform(get("/login?locked"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Trop de tentatives")));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMINISTRATEUR", "PROFIL_MENU", "PROFIL_USER", "PROFIL_SOUSMENU", "MARCHAND"})
    void unknownPhoneOnUpdateIsReportedAsError() throws Exception {
        Mockito.when(usersService.updateEmail(anyString(), anyString())).thenReturn(0);
        mvc.perform(post("/parametres/modif-email").with(csrf())
                .param("telephone", "99999999").param("email", "a@b.tg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attributeExists("error"));
    }
}
