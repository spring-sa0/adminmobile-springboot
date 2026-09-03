package com.laposte.adminmobile.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;

import java.util.List;

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
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/parametres/**").hasRole("ADMINISTRATEUR")
                .requestMatchers("/annulation-op/**").hasRole("ADMINISTRATEUR")
                .requestMatchers("/gateway/**").hasRole("ADMINISTRATEUR")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation().changeSessionId()
                .maximumSessions(1)
            )
            // Les ecrans historiques (releve Excel/PDF, Jasper) sont des formulaires
            // classiques non-AJAX ; CSRF Spring Boot par defaut reste actif pour eux.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        return http.build();
    }
}
