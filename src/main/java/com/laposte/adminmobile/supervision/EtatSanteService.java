package com.laposte.adminmobile.supervision;

import com.laposte.adminmobile.security.LoginAttemptService;
import com.laposte.adminmobile.web.ChargeFilter;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Etat de sante de l'application : base Oracle, compte technique, pool de connexions, memoire,
 * CPU, disque, charge, erreurs, trafic HTTP, connexions. Chaque indicateur est evalue
 * independamment : l'echec de l'un (ex. base injoignable) n'empeche pas d'afficher les autres.
 */
@Service
public class EtatSanteService {

    public enum Niveau {
        OK, ALERTE, CRITIQUE, INCONNU;

        Niveau pire(Niveau autre) {
            return rang(autre) > rang(this) ? autre : this;
        }

        private static int rang(Niveau n) {
            return switch (n) {
                case OK -> 0;
                case INCONNU -> 1;
                case ALERTE -> 2;
                case CRITIQUE -> 3;
            };
        }
    }

    /**
     * @param code    identifiant stable (pour les alertes)
     * @param nom     libelle affiche
     * @param valeur  mesure courante
     * @param niveau  evaluation
     * @param detail  precision sur la mesure
     * @param conseil quoi faire si le niveau n'est pas OK (vide sinon)
     */
    public record Indicateur(String code, String nom, String valeur, Niveau niveau, String detail, String conseil) {
    }

    public record EtatSante(Niveau niveau, Date horodatage, String version, Date demarrage, String dureeFonctionnement,
                            List<Indicateur> indicateurs) {
    }

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final ObjectProvider<MeterRegistry> meterRegistry;
    private final ObjectProvider<ChargeFilter> chargeFilter;
    private final ObjectProvider<LoginAttemptService> loginAttemptService;
    private final File dossierJournaux;

    public EtatSanteService(DataSource dataSource,
                            ObjectProvider<MeterRegistry> meterRegistry,
                            ObjectProvider<ChargeFilter> chargeFilter,
                            ObjectProvider<LoginAttemptService> loginAttemptService,
                            @Value("${app.logs.dir:logs}") String dossierJournaux) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcTemplate.setQueryTimeout(5);
        this.meterRegistry = meterRegistry;
        this.chargeFilter = chargeFilter;
        this.loginAttemptService = loginAttemptService;
        this.dossierJournaux = new File(dossierJournaux);
    }

    public EtatSante etat() {
        List<Indicateur> indicateurs = new ArrayList<>();
        indicateurs.add(mesurer("BASE", "Base Oracle", this::base));
        indicateurs.add(mesurer("COMPTE_ORACLE", "Compte Oracle de l'application", this::compteOracle));
        indicateurs.add(mesurer("POOL", "Connexions Oracle (pool)", this::pool));
        indicateurs.add(mesurer("MEMOIRE", "Memoire Java", this::memoire));
        indicateurs.add(mesurer("CPU", "Processeur", this::cpu));
        indicateurs.add(mesurer("DISQUE", "Disque (journaux)", this::disque));
        indicateurs.add(mesurer("CHARGE", "Charge (recherches / exports lourds)", this::charge));
        indicateurs.add(mesurer("ERREURS", "Erreurs", this::erreurs));
        indicateurs.add(mesurer("TRAFIC", "Trafic HTTP (depuis le demarrage)", this::trafic));
        indicateurs.add(mesurer("CONNEXIONS", "Connexions a l'interface", this::connexions));

        Niveau global = Niveau.OK;
        for (Indicateur i : indicateurs) {
            global = global.pire(i.niveau());
        }
        long demarrage = ManagementFactory.getRuntimeMXBean().getStartTime();
        String version = EtatSanteService.class.getPackage().getImplementationVersion();
        return new EtatSante(global, new Date(), version == null ? "dev" : version, new Date(demarrage),
                duree(Duration.ofMillis(System.currentTimeMillis() - demarrage)), indicateurs);
    }

    /** Indicateur dont le calcul a echoue : niveau inconnu plutot qu'une page de supervision en erreur. */
    private Indicateur mesurer(String code, String nom, Supplier<Indicateur> calcul) {
        try {
            return calcul.get();
        } catch (RuntimeException e) {
            return new Indicateur(code, nom, "?", Niveau.INCONNU, "Mesure impossible : " + e.getMessage(), "");
        }
    }

    // ---- Base de donnees -------------------------------------------------------------------

    private Indicateur base() {
        long ms;
        // On ne chronometre que la requete : l'ouverture d'une connexion (premier appel apres un
        // demarrage) fausserait la mesure, et l'attente d'une connexion est suivie par l'indicateur du pool.
        try (java.sql.Connection connexion = dataSource.getConnection();
             java.sql.Statement requete = connexion.createStatement()) {
            requete.setQueryTimeout(5);
            long t0 = System.nanoTime();
            requete.execute("SELECT 1 FROM DUAL");
            ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
        } catch (java.sql.SQLException | RuntimeException e) {
            Diagnostic d = CatalogueDiagnostics.diagnostiquer(messageComplet(e));
            return new Indicateur("BASE", "Base Oracle", "Injoignable", Niveau.CRITIQUE,
                    d.titre() + " : " + racine(e).getMessage(), String.join(" ", d.actions()));
        }
        Niveau n = ms > 1000 ? Niveau.ALERTE : Niveau.OK;
        return new Indicateur("BASE", "Base Oracle", ms + " ms", n, "Temps de reponse d'une requete simple",
                n == Niveau.OK ? "" : "Base lente : verifier avec le DBA la charge de l'instance et le reseau.");
    }

    private Indicateur compteOracle() {
        Map<String, Object> compte = jdbcTemplate.queryForMap("SELECT account_status, expiry_date FROM user_users");
        String statut = String.valueOf(compte.get("ACCOUNT_STATUS"));
        Date expiration = (Date) compte.get("EXPIRY_DATE");
        String conseil = "Faire changer le mot de passe du compte DB_USERNAME par le DBA, mettre a jour DB_PASSWORD "
                + "dans .env, puis redemarrer (docker compose up -d).";
        if (statut.contains("LOCKED")) {
            return new Indicateur("COMPTE_ORACLE", "Compte Oracle de l'application", statut, Niveau.CRITIQUE,
                    "Compte verrouille", "Demander au DBA : ALTER USER <compte> ACCOUNT UNLOCK, puis verifier DB_PASSWORD.");
        }
        if (statut.contains("EXPIRED")) {
            return new Indicateur("COMPTE_ORACLE", "Compte Oracle de l'application", statut, Niveau.CRITIQUE,
                    "Mot de passe expire : a la fin de la periode de grace, l'application ne pourra plus se connecter.", conseil);
        }
        if (expiration != null) {
            long jours = TimeUnit.MILLISECONDS.toDays(expiration.getTime() - System.currentTimeMillis());
            String date = new SimpleDateFormat("dd/MM/yyyy").format(expiration);
            if (jours <= 15) {
                return new Indicateur("COMPTE_ORACLE", "Compte Oracle de l'application", statut, Niveau.ALERTE,
                        "Le mot de passe expire le " + date + " (dans " + jours + " jour(s)).", conseil);
            }
            return new Indicateur("COMPTE_ORACLE", "Compte Oracle de l'application", statut, Niveau.OK,
                    "Mot de passe valable jusqu'au " + date, "");
        }
        return new Indicateur("COMPTE_ORACLE", "Compte Oracle de l'application", statut, Niveau.OK,
                "Pas de date d'expiration du mot de passe", "");
    }

    private Indicateur pool() {
        if (!(dataSource instanceof HikariDataSource hikari) || hikari.getHikariPoolMXBean() == null) {
            return new Indicateur("POOL", "Connexions Oracle (pool)", "?", Niveau.INCONNU, "Pool non initialise", "");
        }
        HikariPoolMXBean p = hikari.getHikariPoolMXBean();
        int max = hikari.getMaximumPoolSize();
        int actives = p.getActiveConnections();
        int attente = p.getThreadsAwaitingConnection();
        Niveau n = attente > 0 || actives >= max ? Niveau.CRITIQUE : actives >= 0.8 * max ? Niveau.ALERTE : Niveau.OK;
        return new Indicateur("POOL", "Connexions Oracle (pool)", actives + " / " + max + " utilisees",
                n, attente + " requete(s) en attente d'une connexion, " + p.getIdleConnections() + " libre(s)",
                n == Niveau.OK ? "" : "Pool sature : traitements lents ou trop nombreux. Voir la latence Oracle et la charge ; "
                        + "abaisser CHARGE_TRAITEMENTS_LOURDS ou augmenter DB_POOL_SIZE avec l'accord du DBA.");
    }

    // ---- Ressources du serveur ---------------------------------------------------------------

    private Indicateur memoire() {
        Runtime rt = Runtime.getRuntime();
        long utilisee = rt.totalMemory() - rt.freeMemory();
        long max = rt.maxMemory();
        double taux = (double) utilisee / max;
        Niveau n = taux > 0.95 ? Niveau.CRITIQUE : taux > 0.85 ? Niveau.ALERTE : Niveau.OK;
        return new Indicateur("MEMOIRE", "Memoire Java", pourcent(taux), n, mo(utilisee) + " utilises sur " + mo(max),
                n == Niveau.OK ? "" : "Memoire presque pleine : gros etats ou exports simultanes. Abaisser "
                        + "CHARGE_TRAITEMENTS_LOURDS ou EXPORT_MAX_LIGNES_EXCEL, ou augmenter la RAM (JAVA_OPTS).");
    }

    private Indicateur cpu() {
        if (!(ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os)) {
            return new Indicateur("CPU", "Processeur", "?", Niveau.INCONNU, "Mesure non disponible sur cette JVM", "");
        }
        double app = os.getProcessCpuLoad();
        double systeme = os.getCpuLoad();
        if (app < 0) {
            return new Indicateur("CPU", "Processeur", "?", Niveau.INCONNU, "Mesure pas encore disponible", "");
        }
        Niveau n = systeme > 0.85 ? Niveau.ALERTE : Niveau.OK;
        return new Indicateur("CPU", "Processeur", pourcent(app) + " (application)", n,
                pourcent(Math.max(systeme, 0)) + " pour l'ensemble du serveur, " + os.getAvailableProcessors() + " coeur(s)",
                n == Niveau.OK ? "" : "Serveur tres charge : identifier les processus (top) ; si c'est l'application, voir les ecrans lents.");
    }

    private Indicateur disque() {
        File f = dossierJournaux.exists() ? dossierJournaux : new File(".");
        long libre = f.getUsableSpace();
        long total = f.getTotalSpace();
        if (total <= 0) {
            return new Indicateur("DISQUE", "Disque (journaux)", "?", Niveau.INCONNU, "Mesure impossible", "");
        }
        double taux = (double) libre / total;
        Niveau n = taux < 0.03 ? Niveau.CRITIQUE : taux < 0.10 ? Niveau.ALERTE : Niveau.OK;
        return new Indicateur("DISQUE", "Disque (journaux)", pourcent(taux) + " libre", n,
                mo(libre) + " libres sur " + mo(total) + " (" + f.getAbsolutePath() + ")",
                n == Niveau.OK ? "" : "Liberer de l'espace : docker image prune, verifier la taille du volume des journaux.");
    }

    // ---- Activite de l'application -----------------------------------------------------------

    private Indicateur charge() {
        ChargeFilter f = chargeFilter.getIfAvailable();
        if (f == null) {
            return new Indicateur("CHARGE", "Charge (recherches / exports lourds)", "?", Niveau.INCONNU, "", "");
        }
        boolean refusRecent = f.getDernierRefus() > System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(15);
        Niveau n = refusRecent ? Niveau.ALERTE : Niveau.OK;
        return new Indicateur("CHARGE", "Charge (recherches / exports lourds)", f.getEnCours() + " / " + f.getMaxSimultanes() + " en cours", n,
                f.getRefus() + " requete(s) refusee(s) pour surcharge depuis le demarrage",
                n == Niveau.OK ? "" : "Des utilisateurs ont recu \"service momentanement indisponible\" dans les 15 dernieres minutes. "
                        + "Si cela se repete : augmenter CHARGE_TRAITEMENTS_LOURDS (et la RAM / DB_POOL_SIZE en consequence).");
    }

    private Indicateur erreurs() {
        JournalErreurs journal = JournalErreurs.instance();
        long maintenant = System.currentTimeMillis();
        long uneHeure = journal.nombreDepuis(maintenant - TimeUnit.HOURS.toMillis(1));
        long unJour = journal.nombreDepuis(maintenant - TimeUnit.DAYS.toMillis(1));
        long critiques = journal.groupes().stream()
                .filter(g -> g.getDerniere() > maintenant - TimeUnit.HOURS.toMillis(1))
                .filter(g -> g.getDiagnostic().gravite() == Diagnostic.Gravite.CRITIQUE)
                .count();
        Niveau n = critiques > 0 ? Niveau.CRITIQUE : uneHeure > 20 ? Niveau.ALERTE : Niveau.OK;
        return new Indicateur("ERREURS", "Erreurs", uneHeure + " sur la derniere heure", n, unJour + " sur 24 h",
                n == Niveau.OK ? "" : "Consulter le journal des erreurs ci-dessous : cause probable et etapes de resolution pour chacune.");
    }

    private Indicateur trafic() {
        MeterRegistry registry = meterRegistry.getIfAvailable();
        if (registry == null) {
            return new Indicateur("TRAFIC", "Trafic HTTP (depuis le demarrage)", "?", Niveau.INCONNU, "", "");
        }
        long total = 0;
        long erreurs = 0;
        double tempsTotalMs = 0;
        double maxMs = 0;
        for (Timer t : registry.find("http.server.requests").timers()) {
            long n = t.count();
            total += n;
            tempsTotalMs += t.totalTime(TimeUnit.MILLISECONDS);
            maxMs = Math.max(maxMs, t.max(TimeUnit.MILLISECONDS));
            if ("SERVER_ERROR".equals(t.getId().getTag("outcome"))) {
                erreurs += n;
            }
        }
        double taux = total == 0 ? 0 : (double) erreurs / total;
        Niveau n = total >= 50 && taux > 0.05 ? Niveau.ALERTE : Niveau.OK;
        return new Indicateur("TRAFIC", "Trafic HTTP (depuis le demarrage)", total + " requete(s)", n,
                erreurs + " en erreur serveur (" + pourcent(taux) + "), temps moyen "
                        + (total == 0 ? 0 : Math.round(tempsTotalMs / total)) + " ms",
                n == Niveau.OK ? "" : "Plus de 5 % de requetes en erreur : voir le journal des erreurs.");
    }

    private Indicateur connexions() {
        LoginAttemptService l = loginAttemptService.getIfAvailable();
        if (l == null) {
            return new Indicateur("CONNEXIONS", "Connexions a l'interface", "?", Niveau.INCONNU, "", "");
        }
        long bloques = l.nombreBloques();
        Niveau n = bloques > 5 ? Niveau.ALERTE : Niveau.OK;
        return new Indicateur("CONNEXIONS", "Connexions a l'interface", bloques + " blocage(s) en cours", n,
                "Identifiants ou adresses IP bloques temporairement apres trop d'echecs (voir audit.log : CONNEXION_ECHEC)",
                n == Niveau.OK ? "" : "Nombreux blocages : tentative d'intrusion possible, ou probleme general de mot de passe. "
                        + "Consulter audit.log (CONNEXION_ECHEC / CONNEXION_BLOQUEE).");
    }

    // ---- Utilitaires -------------------------------------------------------------------------

    private static Throwable racine(Throwable e) {
        Throwable r = e;
        while (r.getCause() != null && r.getCause() != r) {
            r = r.getCause();
        }
        return r;
    }

    private static String messageComplet(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            sb.append(t.getClass().getName()).append(' ').append(t.getMessage()).append(' ');
        }
        return sb.toString();
    }

    private static String pourcent(double taux) {
        return Math.round(taux * 100) + " %";
    }

    private static String mo(long octets) {
        return octets >= 1L << 30 ? String.format("%.1f Go", octets / (double) (1L << 30)) : (octets >> 20) + " Mo";
    }

    private static String duree(Duration d) {
        long jours = d.toDays();
        return (jours > 0 ? jours + " j " : "") + d.toHoursPart() + " h " + d.toMinutesPart() + " min";
    }
}
