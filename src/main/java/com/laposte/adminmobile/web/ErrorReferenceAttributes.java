package com.laposte.adminmobile.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ajoute aux pages d'erreur serveur (5xx) une reference courte, affichee a l'utilisateur et
 * journalisee avec l'exception : l'utilisateur peut la communiquer au support, qui retrouve la
 * cause dans les logs. La page elle-meme n'expose toujours ni message, ni trace, ni SQL
 * (server.error.* dans application.yml).
 */
@Component
public class ErrorReferenceAttributes extends DefaultErrorAttributes {

    private static final Logger log = LoggerFactory.getLogger(ErrorReferenceAttributes.class);

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> attributes = super.getErrorAttributes(webRequest, options);
        Object status = attributes.get("status");
        if (status instanceof Integer code && code >= 500) {
            String reference = HexFormat.of().withUpperCase().toHexDigits(ThreadLocalRandom.current().nextInt());
            attributes.put("reference", reference);
            Object path = webRequest.getAttribute("jakarta.servlet.error.request_uri", RequestAttributes.SCOPE_REQUEST);
            log.error("Erreur {} (HTTP {}) sur {}", reference, code, path, getError(webRequest));
        }
        return attributes;
    }
}
