package com.laposte.adminmobile.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Equivalent de login.xhtml + Userconnexion (partie affichage).
 * L'authentification elle-meme est geree par Spring Security
 * (voir security.SecurityConfig et security.LegacyDbAuthenticationProvider).
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
