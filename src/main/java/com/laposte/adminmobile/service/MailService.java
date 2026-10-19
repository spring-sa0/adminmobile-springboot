package com.laposte.adminmobile.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Spring Boot port of {@code admin.mobile.dao.maildao}.
 * <p>
 * The legacy DAO built its own {@code javax.mail.Session}/{@code Transport} by hand for every
 * call, with the SMTP host, account and password hardcoded in source
 * ({@code mail.laposte.tg} / {@code sms.contact@laposte.tg}). That configuration now lives in
 * {@code application.yml} under {@code spring.mail.*} (host/port/username/password, injected via
 * the {@code MAIL_HOST}/{@code MAIL_USERNAME}/{@code MAIL_PASSWORD} environment variables) and is
 * wired into Spring's auto-configured {@link JavaMailSender} - this service only assembles the
 * message. TODO: set MAIL_USERNAME/MAIL_PASSWORD/MAIL_HOST in the deployment environment to the
 * real SMTP credentials before relying on this in production.
 */
@Service
public class MailService {

    /** Expediteur historique de l'application (maildao). */
    static final String EXPEDITEUR_PAR_DEFAUT = "sms.contact@laposte.tg";

    private final JavaMailSender mailSender;
    private final String from;
    private final boolean configure;

    /**
     * Expediteur : MAIL_FROM s'il est renseigne, sinon MAIL_USERNAME si c'est une adresse, sinon
     * l'expediteur historique. (Auparavant, un MAIL_USERNAME vide donnait un expediteur vide et tous
     * les envois echouaient : "Illegal address in string ''".)
     */
    @Autowired
    public MailService(JavaMailSender mailSender,
                       @Value("${app.mail.expediteur:}") String expediteur,
                       @Value("${spring.mail.username:}") String utilisateur,
                       @Value("${spring.mail.host:}") String serveur) {
        this.mailSender = mailSender;
        this.from = !isBlank(expediteur) ? expediteur.trim()
                : !isBlank(utilisateur) && utilisateur.contains("@") ? utilisateur.trim()
                : EXPEDITEUR_PAR_DEFAUT;
        this.configure = !isBlank(serveur);
    }

    /** Vrai si un serveur SMTP est renseigne (MAIL_HOST) : sinon aucun envoi ne peut aboutir. */
    public boolean estConfigure() {
        return configure;
    }

    public String getExpediteur() {
        return from;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Direct port of {@code maildao.sendMessage(subject, text, destinataire)}: a single
     * plain-text recipient, no attachments, no CC (both were present but commented out in the
     * legacy code).
     */
    public void sendMessage(String subject, String text, String destinataire) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(destinataire);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
