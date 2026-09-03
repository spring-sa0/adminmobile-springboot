package com.laposte.adminmobile.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Equivalent de index.xhtml / content.xhtml : page d'accueil du back-office
 * une fois authentifie.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }
}
