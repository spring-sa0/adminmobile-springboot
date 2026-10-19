package com.laposte.adminmobile.supervision;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Active les taches planifiees (controle des alertes de supervision chaque minute). */
@Configuration
@EnableScheduling
public class SupervisionConfig {
}
