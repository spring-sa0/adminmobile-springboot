package com.laposte.adminmobile.supervision;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Appender Logback qui transmet a {@link JournalErreurs} toutes les erreurs de l'application :
 * evenements ERROR de n'importe quelle classe, et WARN accompagnes d'une exception dans le code de
 * l'application (ex. echec d'envoi de SMS ou d'e-mail). Declare dans logback-spring.xml.
 * <p>
 * Le contexte de la requete (URL, utilisateur, reference de la page 500) vient du MDC, renseigne
 * par {@code web.ContexteJournalFilter} et {@code web.ErrorReferenceAttributes}.
 */
public class JournalErreursAppender extends AppenderBase<ILoggingEvent> {

    /** Loggers qui repetent une erreur deja journalisee ailleurs avec sa trace complete. */
    private static final Set<String> LOGGERS_DOUBLONS = Set.of(
            // Message SQL seul ; l'exception complete est journalisee ensuite par ErrorReferenceAttributes.
            "org.hibernate.engine.jdbc.spi.SqlExceptionHelper",
            "AUDIT");
    /** Tomcat journalise aussi l'exception d'une requete, deja traitee par la page d'erreur. */
    private static final String PREFIXE_TOMCAT_DISPATCHER = "org.apache.catalina.core.ContainerBase";

    private static final int TRACE_MAX = 12_000;
    private static final int MESSAGE_MAX = 2_000;

    /** Nombres variables (numeros d'operation, montants...) ; les codes ORA-/PLS- sont conserves. */
    private static final Pattern NOMBRES = Pattern.compile("(?<!(?:ORA|PLS)-\\d{0,4})\\d+");
    private static final Pattern REFERENCES = Pattern.compile("\\b[0-9A-F]{8}\\b");

    private String dossier;

    /** Dossier des journaux (LOG_DIR), renseigne par logback-spring.xml. */
    public void setDossier(String dossier) {
        this.dossier = dossier;
    }

    @Override
    public void start() {
        if (dossier != null && !dossier.isBlank()) {
            JournalErreurs.instance().configurer(Path.of(dossier));
        }
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        try {
            if (!retenir(event)) {
                return;
            }
            JournalErreurs.instance().enregistrer(convertir(event));
        } catch (RuntimeException e) {
            // Ne jamais faire echouer la journalisation de l'application.
            System.err.println("Supervision : evenement non enregistre : " + e);
        }
    }

    static boolean retenir(ILoggingEvent event) {
        String logger = event.getLoggerName();
        if (LOGGERS_DOUBLONS.contains(logger) || logger.startsWith(PREFIXE_TOMCAT_DISPATCHER)) {
            return false;
        }
        if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
            return true;
        }
        return event.getLevel() == Level.WARN && event.getThrowableProxy() != null && logger.startsWith("com.laposte");
    }

    static EvenementErreur convertir(ILoggingEvent event) {
        IThrowableProxy exception = event.getThrowableProxy();
        IThrowableProxy racine = exception;
        while (racine != null && racine.getCause() != null && racine.getCause() != racine) {
            racine = racine.getCause();
        }
        String ligneCode = premiereLigneApplication(racine);
        if (ligneCode == null) {
            ligneCode = premiereLigneApplication(exception);
        }
        String message = tronquer(event.getFormattedMessage(), MESSAGE_MAX);
        String trace = exception == null ? null : tronquer(ThrowableProxyUtil.asString(exception), TRACE_MAX);
        Map<String, String> mdc = event.getMDCPropertyMap();

        // Signature sans numero de ligne : elle doit rester la meme d'une version a l'autre (sinon une
        // erreur marquee resolue reviendrait comme nouvelle apres chaque deploiement).
        String signature = racine != null
                ? racine.getClassName() + "|" + normaliser(racine.getMessage()) + "|" + sansNumeroDeLigne(ligneCode)
                : event.getLoggerName() + "|" + normaliser(message);

        return new EvenementErreur(event.getTimeStamp(), event.getLevel().toString(), event.getLoggerName(), message,
                exception == null ? null : exception.getClassName(),
                racine == null ? null : racine.getClassName(),
                racine == null ? null : tronquer(racine.getMessage(), MESSAGE_MAX),
                ligneCode, trace,
                mdc.get("reference"), mdc.get("url"), mdc.get("utilisateur"), signature);
    }

    private static String premiereLigneApplication(IThrowableProxy proxy) {
        if (proxy == null || proxy.getStackTraceElementProxyArray() == null) {
            return null;
        }
        for (StackTraceElementProxy ligne : proxy.getStackTraceElementProxyArray()) {
            StackTraceElement e = ligne.getStackTraceElement();
            if (e.getClassName().startsWith("com.laposte.") && !e.getClassName().contains("$$")) {
                return e.getClassName() + "." + e.getMethodName() + "(" + e.getFileName() + ":" + e.getLineNumber() + ")";
            }
        }
        return null;
    }

    /** "pkg.Classe.methode(Classe.java:42)" -> "pkg.Classe.methode". */
    private static String sansNumeroDeLigne(String ligneCode) {
        if (ligneCode == null) {
            return "";
        }
        int parenthese = ligneCode.indexOf('(');
        return parenthese < 0 ? ligneCode : ligneCode.substring(0, parenthese);
    }

    /** Message sans ses parties variables, pour regrouper les erreurs de meme cause. */
    static String normaliser(String message) {
        if (message == null) {
            return "";
        }
        String m = REFERENCES.matcher(message).replaceAll("REF");
        m = NOMBRES.matcher(m).replaceAll("#");
        m = m.replaceAll("\\s+", " ").trim();
        return m.length() > 300 ? m.substring(0, 300) : m;
    }

    private static String tronquer(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max) + "\n... (tronque)";
    }
}
