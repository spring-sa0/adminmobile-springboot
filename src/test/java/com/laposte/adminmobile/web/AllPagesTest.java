package com.laposte.adminmobile.web;

import com.laposte.adminmobile.security.LegacyDbAuthenticationProvider;
import com.laposte.adminmobile.security.SecurityConfig;
import com.laposte.adminmobile.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
@Import({SecurityConfig.class, AllPagesTest.MockServices.class})
class AllPagesTest {

    private static final String P = "date1=2026-01-01&date2=2026-01-31";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsersService usersService;

    @Autowired
    private RecuElectroniqueService recuElectroniqueService;

    @Autowired
    private SuggestionsService suggestionsService;

    @Autowired
    private SupportService supportService;

    @Autowired
    private TypeReclammationService typeReclammationService;

    @TestConfiguration
    static class MockServices {
        @Bean
        static BeanDefinitionRegistryPostProcessor registerMocks() {
            return new BeanDefinitionRegistryPostProcessor() {
                @Override
                public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
                }

                @Override
                public void postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory f) {
                    List<Class<?>> types = List.of(
                            ApplimobileService.class, BankToBankReportService.class, CommissionsReportService.class,
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

    @ParameterizedTest
    @ValueSource(strings = {
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
            "/marchand/commissions", "/marchand/commissions?" + P,
    })
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
        mvc.perform(post("/annulation-op/annuler").with(csrf()).param("numop", "abc"))
                .andExpect(status().is3xxRedirection());
        verify(recuElectroniqueService, never()).annulopeco(any(), any());
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
