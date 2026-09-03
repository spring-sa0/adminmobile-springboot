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

    private final JavaMailSender mailSender;
    private final String from;

    @Autowired
    public MailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username:sms.contact@laposte.tg}") String from) {
        this.mailSender = mailSender;
        this.from = from;
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
