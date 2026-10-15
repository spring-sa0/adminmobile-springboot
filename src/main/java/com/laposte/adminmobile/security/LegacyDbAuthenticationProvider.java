package com.laposte.adminmobile.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Portage fidele de la logique historique de {@code Userconnexion.connecterUser()}.
 *
 * IMPORTANT (a valider avec l'equipe) : l'application d'origine n'authentifie PAS contre
 * un mot de passe stocke applicativement (le champ USERS_EPOSTE.PWD n'est en realite pas
 * verifie dans le chemin de code actif). Elle ouvre une connexion JDBC brute vers Oracle
 * en utilisant le login/mot de passe saisis comme identifiants de connexion a la base
 * elle-meme, puis va chercher le profil dans MBUDGET.UTILISATEUR_LOG.CODE_PROFIL.
 * Concretement : chaque utilisateur de l'appli doit avoir un compte Oracle valide.
 *
 * Ce comportement est conserve ici a l'identique pour la fidelite du portage, mais c'est
 * une architecture d'authentification inhabituelle et couplee fortement a la base de
 * donnees : a moyen terme, il est recommande de migrer vers un stockage applicatif des
 * utilisateurs (table USERS_EPOSTE + mot de passe hache BCrypt) plutot que des comptes
 * Oracle par utilisateur.
 */
@Component
public class LegacyDbAuthenticationProvider implements AuthenticationProvider {

    /**
     * Nom d'utilisateur Oracle non quote : lettres, chiffres, _ $ #. Refuse aussi les syntaxes que
     * le driver interprete dans le nom ("sys as sysdba", connexion proxy "user[cible]").
     */
    private static final Pattern ORACLE_LOGIN = Pattern.compile("[A-Za-z][A-Za-z0-9_$#]{0,127}");

    private final String jdbcUrl;
    private final String driverClassName;
    private final String technicalAccount;
    private final LoginAttemptService loginAttemptService;

    public LegacyDbAuthenticationProvider(
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.driver-class-name}") String driverClassName,
            @Value("${spring.datasource.username:}") String technicalAccount,
            LoginAttemptService loginAttemptService) {
        this.jdbcUrl = jdbcUrl;
        this.driverClassName = driverClassName;
        this.technicalAccount = technicalAccount;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String login = authentication.getName() == null ? "" : authentication.getName().trim();
        String password = (String) authentication.getCredentials();
        String ip = authentication.getDetails() instanceof WebAuthenticationDetails details
                ? details.getRemoteAddress() : null;

        if (loginAttemptService.isBlocked(login, ip)) {
            Audit.evenement("CONNEXION_BLOQUEE", login, ip, null);
            throw new LockedException("Trop de tentatives de connexion");
        }

        // Le compte technique de l'application (spring.datasource.username) ne doit jamais servir
        // a se connecter a l'interface : des essais rates le feraient verrouiller par Oracle et
        // couperaient toute l'application.
        boolean refused = !ORACLE_LOGIN.matcher(login).matches()
                || password == null || password.isEmpty()
                || (!technicalAccount.isBlank() && technicalAccount.equalsIgnoreCase(login));

        Integer idProfil = refused ? null : openConnectionAndFetchProfil(login, password);
        if (idProfil == null) {
            loginAttemptService.loginFailed(login, ip);
            Audit.evenement("CONNEXION_ECHEC", login, ip, Map.of("motif", refused ? "identifiant refuse" : "identifiants invalides"));
            throw new BadCredentialsException("Utilisateur ou mot de passe incorrect");
        }

        List<GrantedAuthority> authorities = authoritiesForProfil(idProfil);
        if (authorities.isEmpty()) {
            loginAttemptService.loginFailed(login, ip);
            Audit.evenement("CONNEXION_ECHEC", login, ip, Map.of("motif", "profil inconnu " + idProfil));
            throw new BadCredentialsException("Profil inconnu (" + idProfil + ")");
        }

        loginAttemptService.loginSucceeded(login);
        Audit.evenement("CONNEXION", login, ip, Map.of("profil", String.valueOf(idProfil)));
        return new UsernamePasswordAuthenticationToken(login, null, authorities);
    }

    private Integer openConnectionAndFetchProfil(String login, String password) {
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Driver JDBC introuvable: " + driverClassName, e);
        }

        Properties props = new Properties();
        props.setProperty("user", login);
        props.setProperty("password", password);
        // Sans delai, une base lente ou injoignable bloque le thread de la requete de login indefiniment.
        props.setProperty("oracle.net.CONNECT_TIMEOUT", "10000");
        props.setProperty("oracle.jdbc.ReadTimeout", "30000");

        try (Connection connection = DriverManager.getConnection(jdbcUrl, props)) {
            String requete = "SELECT CODE_PROFIL FROM MBUDGET.UTILISATEUR_LOG WHERE lower(LOGIN) = ?";
            try (PreparedStatement ps = connection.prepareStatement(requete)) {
                ps.setString(1, login == null ? null : login.toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            return null;
        } catch (SQLException e) {
            // Identifiants Oracle invalides ou profil introuvable -> echec d'authentification.
            return null;
        }
    }

    /**
     * Reproduit exactement les combinaisons de flags de Userconnexion.connecterUser():
     *  - idprofil 1 (administrateur)  -> profilmenu, profiluser, profilsousmenu, marchand
     *  - idprofil 2 (utilisateur)     -> profiluser, marchand
     *  - idprofil 3 (consultation)    -> profilsousmenu, marchand
     *  - idprofil 9 (marchand)        -> marchand
     */
    private List<GrantedAuthority> authoritiesForProfil(int idProfil) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        switch (idProfil) {
            case 1:
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMINISTRATEUR"));
                authorities.add(new SimpleGrantedAuthority("PROFIL_MENU"));
                authorities.add(new SimpleGrantedAuthority("PROFIL_USER"));
                authorities.add(new SimpleGrantedAuthority("PROFIL_SOUSMENU"));
                authorities.add(new SimpleGrantedAuthority("MARCHAND"));
                break;
            case 2:
                authorities.add(new SimpleGrantedAuthority("ROLE_UTILISATEUR"));
                authorities.add(new SimpleGrantedAuthority("PROFIL_USER"));
                authorities.add(new SimpleGrantedAuthority("MARCHAND"));
                break;
            case 3:
                authorities.add(new SimpleGrantedAuthority("ROLE_CONSULTATION"));
                authorities.add(new SimpleGrantedAuthority("PROFIL_SOUSMENU"));
                authorities.add(new SimpleGrantedAuthority("MARCHAND"));
                break;
            case 9:
                authorities.add(new SimpleGrantedAuthority("ROLE_MARCHAND"));
                authorities.add(new SimpleGrantedAuthority("MARCHAND"));
                break;
            default:
                // profil inconnu -> aucune autorite, l'appelant refuse l'authentification.
                break;
        }
        return authorities;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
