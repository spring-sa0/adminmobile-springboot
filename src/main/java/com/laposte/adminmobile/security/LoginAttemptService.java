package com.laposte.adminmobile.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite les tentatives de connexion, par identifiant et par adresse IP, AVANT d'ouvrir une
 * connexion Oracle. Sans ce garde-fou, chaque essai part directement sur la base : un attaquant
 * peut tester des mots de passe sans limite, et surtout faire verrouiller les comptes Oracle par
 * le profil de la base (FAILED_LOGIN_ATTEMPTS) - y compris des comptes utilises par d'autres
 * applications.
 * <p>
 * Etat en memoire : il est remis a zero au redemarrage et n'est pas partage entre plusieurs
 * instances, ce qui suffit pour une instance unique.
 */
@Component
public class LoginAttemptService {

    /** Au-dela, on purge les entrees expirees pour qu'une rafale d'identifiants inventes ne sature pas la memoire. */
    private static final int PURGE_THRESHOLD = 10_000;

    private final int maxAttemptsPerLogin;
    private final int maxAttemptsPerIp;
    private final Duration lockDuration;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${app.security.max-login-attempts:5}") int maxAttemptsPerLogin,
            @Value("${app.security.max-login-attempts-per-ip:20}") int maxAttemptsPerIp,
            @Value("${app.security.login-lock-duration:15m}") Duration lockDuration) {
        this.maxAttemptsPerLogin = maxAttemptsPerLogin;
        this.maxAttemptsPerIp = maxAttemptsPerIp;
        this.lockDuration = lockDuration;
    }

    public boolean isBlocked(String login, String ip) {
        return isBlocked(loginKey(login), maxAttemptsPerLogin) || isBlocked(ipKey(ip), maxAttemptsPerIp);
    }

    public void loginFailed(String login, String ip) {
        if (attempts.size() > PURGE_THRESHOLD) {
            Instant now = Instant.now();
            attempts.values().removeIf(a -> a.expiresAt.isBefore(now));
        }
        increment(loginKey(login));
        if (ip != null) {
            increment(ipKey(ip));
        }
    }

    /** Une connexion reussie efface le compteur de l'identifiant (pas celui de l'IP). */
    public void loginSucceeded(String login) {
        attempts.remove(loginKey(login));
    }

    private boolean isBlocked(String key, int max) {
        if (key == null) {
            return false;
        }
        Attempts a = attempts.get(key);
        if (a == null) {
            return false;
        }
        if (a.expiresAt.isBefore(Instant.now())) {
            attempts.remove(key, a);
            return false;
        }
        return a.count >= max;
    }

    private void increment(String key) {
        Instant now = Instant.now();
        attempts.compute(key, (k, a) -> (a == null || a.expiresAt.isBefore(now))
                ? new Attempts(1, now.plus(lockDuration))
                : new Attempts(a.count + 1, now.plus(lockDuration)));
    }

    private static String loginKey(String login) {
        return login == null ? null : "u:" + login.trim().toLowerCase();
    }

    private static String ipKey(String ip) {
        return ip == null ? null : "ip:" + ip;
    }

    private record Attempts(int count, Instant expiresAt) {
    }
}
