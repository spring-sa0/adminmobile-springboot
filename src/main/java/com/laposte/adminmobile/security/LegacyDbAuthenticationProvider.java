package com.laposte.adminmobile.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

    private final String jdbcUrl;
    private final String driverClassName;

    public LegacyDbAuthenticationProvider(
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.driver-class-name}") String driverClassName) {
        this.jdbcUrl = jdbcUrl;
        this.driverClassName = driverClassName;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String login = authentication.getName();
        String password = (String) authentication.getCredentials();

        Integer idProfil = openConnectionAndFetchProfil(login, password);
        if (idProfil == null) {
            throw new BadCredentialsException("Utilisateur ou mot de passe incorrect");
        }

        List<GrantedAuthority> authorities = authoritiesForProfil(idProfil);
        if (authorities.isEmpty()) {
            throw new BadCredentialsException("Profil inconnu (" + idProfil + ")");
        }

        return new UsernamePasswordAuthenticationToken(login, null, authorities);
    }

    private Integer openConnectionAndFetchProfil(String login, String password) {
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Driver JDBC introuvable: " + driverClassName, e);
        }

        try (Connection connection = DriverManager.getConnection(jdbcUrl, login, password)) {
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
