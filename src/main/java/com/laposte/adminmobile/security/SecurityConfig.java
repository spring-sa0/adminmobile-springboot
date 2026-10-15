package com.laposte.adminmobile.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.ExceptionMappingAuthenticationFailureHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

import java.util.List;
import java.util.Map;

/**
 * {@code @EnableMethodSecurity} is required for {@code @PreAuthorize} on individual controller
 * methods to be enforced (e.g. {@code TypeReclamationController}, whose /reclamations/types URL
 * falls outside the /parametres/** prefix guarded below).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final LegacyDbAuthenticationProvider legacyDbAuthenticationProvider;

    public SecurityConfig(LegacyDbAuthenticationProvider legacyDbAuthenticationProvider) {
        this.legacyDbAuthenticationProvider = legacyDbAuthenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(List.of(legacyDbAuthenticationProvider));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // /error public : sinon une erreur technique au login (driver, base injoignable...)
                // redirige silencieusement vers /login sans aucun message.
                .requestMatchers("/login", "/error", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                // Healthcheck Docker / supervision : renvoie seulement UP ou DOWN (show-details: never).
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/parametres/**").hasRole("ADMINISTRATEUR")
                .requestMatchers("/annulation-op/**").hasRole("ADMINISTRATEUR")
                .requestMatchers("/gateway/**").hasRole("ADMINISTRATEUR")
                .requestMatchers("/reclamations/types").hasRole("ADMINISTRATEUR")
                // Memes regles que l'affichage du menu (fragments/menu.html) : sans elles, un
                // profil marchand/consultation atteignait tous les ecrans en tapant l'URL.
                .requestMatchers("/reclamations/**", "/suggestions/**", "/consultations/**", "/operations/**",
                        "/comptes/**", "/etats/**", "/eposte/**", "/commissions/**").hasAuthority("PROFIL_USER")
                .requestMatchers("/releves/**").hasAuthority("PROFIL_SOUSMENU")
                .requestMatchers("/marchand/**").hasAuthority("MARCHAND")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureHandler(loginFailureHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .addLogoutHandler((request, response, authentication) -> Audit.evenement("DECONNEXION",
                        authentication != null ? authentication.getName() : null, request.getRemoteAddr(), null))
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation().changeSessionId()
                .maximumSessions(1)
            )
            // CSRF actif partout (valeur par defaut) : l'application n'expose aucune API sans session.
            .csrf(Customizer.withDefaults())
            .headers(headers -> headers
                // 'unsafe-inline' reste necessaire pour les scripts : les templates utilisent des
                // <script> et des onclick/onsubmit en ligne. La politique bloque malgre tout le
                // chargement de scripts externes, l'envoi de donnees vers un autre domaine et
                // l'affichage de l'application dans une iframe (clickjacking).
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; "
                                + "img-src 'self' data:; font-src 'self' data:; connect-src 'self'; "
                                + "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'"))
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.SAME_ORIGIN))
                .frameOptions(frame -> frame.deny())
            );

        return http.build();
    }

    /** Distingue le blocage temporaire (trop d'essais, voir LoginAttemptService) d'un simple echec. */
    private AuthenticationFailureHandler loginFailureHandler() {
        ExceptionMappingAuthenticationFailureHandler handler = new ExceptionMappingAuthenticationFailureHandler();
        handler.setDefaultFailureUrl("/login?error");
        handler.setExceptionMappings(Map.of(LockedException.class.getName(), "/login?locked"));
        return handler;
    }
}
