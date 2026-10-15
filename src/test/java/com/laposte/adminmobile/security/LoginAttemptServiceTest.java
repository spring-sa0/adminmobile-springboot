package com.laposte.adminmobile.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    @Test
    void blocksLoginAfterMaxFailuresAndResetsOnSuccess() {
        LoginAttemptService service = new LoginAttemptService(3, 100, Duration.ofMinutes(15));
        for (int i = 0; i < 3; i++) {
            assertThat(service.isBlocked("Agent1", "10.0.0.1")).isFalse();
            service.loginFailed("agent1", "10.0.0.1");
        }
        assertThat(service.isBlocked("AGENT1", "10.0.0.2")).isTrue();

        service.loginSucceeded("agent1");
        assertThat(service.isBlocked("agent1", "10.0.0.2")).isFalse();
    }

    @Test
    void blocksIpTryingManyLogins() {
        LoginAttemptService service = new LoginAttemptService(100, 3, Duration.ofMinutes(15));
        service.loginFailed("a", "10.0.0.9");
        service.loginFailed("b", "10.0.0.9");
        service.loginFailed("c", "10.0.0.9");
        assertThat(service.isBlocked("d", "10.0.0.9")).isTrue();
        assertThat(service.isBlocked("d", "10.0.0.10")).isFalse();
    }

    @Test
    void blockExpires() {
        // Duree negative = blocage deja expire au moment du controle (evite de dependre de l'horloge).
        LoginAttemptService service = new LoginAttemptService(1, 100, Duration.ofSeconds(-1));
        service.loginFailed("agent1", "10.0.0.1");
        assertThat(service.isBlocked("agent1", "10.0.0.1")).isFalse();
    }

    /**
     * Les identifiants refuses d'emblee (compte technique, syntaxe Oracle speciale) ne doivent
     * jamais ouvrir de connexion : l'URL JDBC pointe ici sur un port ferme, une tentative de
     * connexion serait donc visible (et lente) - et ils comptent quand meme comme des echecs.
     */
    @Test
    void refusedLoginsNeverReachOracleAndCountAsFailures() {
        LoginAttemptService attempts = new LoginAttemptService(2, 100, Duration.ofMinutes(15));
        LegacyDbAuthenticationProvider provider = new LegacyDbAuthenticationProvider(
                "jdbc:oracle:thin:@//127.0.0.1:1/NONE", "oracle.jdbc.OracleDriver", "EPOSTE", attempts);

        assertThatThrownBy(() -> provider.authenticate(new UsernamePasswordAuthenticationToken("sys as sysdba", "x")))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> provider.authenticate(new UsernamePasswordAuthenticationToken("proxy[cible]", "x")))
                .isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() -> provider.authenticate(new UsernamePasswordAuthenticationToken("eposte", "x")))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> provider.authenticate(new UsernamePasswordAuthenticationToken("eposte", "x")))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> provider.authenticate(new UsernamePasswordAuthenticationToken("eposte", "x")))
                .isInstanceOf(LockedException.class);
    }
}
