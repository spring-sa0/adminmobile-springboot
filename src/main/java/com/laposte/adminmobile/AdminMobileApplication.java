package com.laposte.adminmobile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entree de l'application "adminmobile".
 *
 * Portage Spring Boot / Thymeleaf de l'application JSF + PrimeFaces legacy
 * (back-office mobile money EcoCCP / e-poste / Flooz / T-Money de La Poste Togo).
 */
@SpringBootApplication
public class AdminMobileApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminMobileApplication.class, args);
    }
}
