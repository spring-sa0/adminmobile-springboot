package com.laposte.adminmobile.supervision;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Registre des erreurs de l'application, alimente par {@link JournalErreursAppender} (donc par
 * tout ce qui est journalise en ERROR, quelle que soit la classe) et consulte par la supervision.
 * <p>
 * Les erreurs sont regroupees par cause ({@link EvenementErreur#signature()}) et diagnostiquees
 * ({@link CatalogueDiagnostics}). Elles sont aussi ecrites sur disque (un fichier JSON par jour
 * dans {@code <LOG_DIR>/erreurs}, 30 jours conserves) et rechargees au demarrage : l'historique
 * survit a un redemarrage, y compris apres un arret brutal (saturation memoire).
 * <p>
 * Instance unique partagee entre Logback (qui demarre avant Spring) et les beans de supervision.
 */
public final class JournalErreurs {

    private static final JournalErreurs INSTANCE = new JournalErreurs();

    /** Au-dela, les groupes les moins recemment vus sont oublies (ils restent dans les fichiers). */
    static final int GROUPES_MAX = 500;
    static final int CONSERVATION_JOURS = 30;
    private static final ZoneId ZONE = ZoneId.systemDefault();

    /** Horodatages des dernieres erreurs, toutes causes confondues, pour mesurer le taux d'erreurs. */
    static final int HORODATAGES_MAX = 20_000;

    private final Map<String, GroupeErreurs> groupes = new LinkedHashMap<>();
    private final java.util.ArrayDeque<Long> horodatages = new java.util.ArrayDeque<>();
    private final ObjectMapper json = new ObjectMapper();
    private final long demarrage = System.currentTimeMillis();
    private Path dossier;
    private long sequence;

    /** Visible pour les tests ; l'application utilise {@link #instance()}. */
    JournalErreurs() {
    }

    public static JournalErreurs instance() {
        return INSTANCE;
    }

    /** Dossier de persistance ; recharge l'historique et purge les fichiers trop anciens. */
    public synchronized void configurer(Path dossierJournaux) {
        if (dossierJournaux == null || dossierJournaux.equals(this.dossier)) {
            return;
        }
        this.dossier = dossierJournaux.resolve("erreurs");
        try {
            Files.createDirectories(dossier);
            purger();
            recharger();
            chargerResolutions();
        } catch (IOException e) {
            System.err.println("Supervision : dossier des erreurs inutilisable (" + dossier + ") : " + e.getMessage());
            this.dossier = null;
        }
    }

    public synchronized void enregistrer(EvenementErreur e) {
        ajouterEnMemoire(e);
        ecrire(e);
    }

    private void ajouterEnMemoire(EvenementErreur e) {
        GroupeErreurs groupe = groupes.get(e.signature());
        if (groupe == null) {
            if (groupes.size() >= GROUPES_MAX) {
                groupes.values().stream().min(Comparator.comparingLong(GroupeErreurs::getDerniere))
                        .ifPresent(g -> groupes.remove(g.getSignature()));
            }
            String texte = String.join(" ", nn(e.message()), nn(e.exception()), nn(e.causeRacine()),
                    nn(e.messageCause()), nn(e.trace()));
            groupe = new GroupeErreurs(Long.toString(++sequence, 36).toUpperCase(), e, CatalogueDiagnostics.diagnostiquer(texte));
            groupes.put(e.signature(), groupe);
        }
        groupe.ajouter(e);
        horodatages.addLast(e.horodatage());
        while (horodatages.size() > HORODATAGES_MAX) {
            horodatages.removeFirst();
        }
    }

    private void ecrire(EvenementErreur e) {
        if (dossier == null) {
            return;
        }
        Path fichier = dossier.resolve("erreurs-" + LocalDate.ofInstant(Instant.ofEpochMilli(e.horodatage()), ZONE) + ".jsonl");
        try (BufferedWriter w = Files.newBufferedWriter(fichier, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(json.writeValueAsString(e));
            w.newLine();
        } catch (IOException ex) {
            // Jamais d'exception ni de journalisation ici : on est appele depuis le systeme de logs.
            System.err.println("Supervision : ecriture impossible dans " + fichier + " : " + ex.getMessage());
        }
    }

    private void recharger() throws IOException {
        try (Stream<Path> fichiers = Files.list(dossier)) {
            List<Path> tries = fichiers.filter(p -> p.getFileName().toString().endsWith(".jsonl")).sorted().toList();
            for (Path f : tries) {
                for (String ligne : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                    try {
                        if (!ligne.isBlank()) {
                            ajouterEnMemoire(json.readValue(ligne, EvenementErreur.class));
                        }
                    } catch (IOException ignoree) {
                        // ligne tronquee (arret brutal en cours d'ecriture) : ignoree
                    }
                }
            }
        }
    }

    private void purger() throws IOException {
        LocalDate limite = LocalDate.now(ZONE).minusDays(CONSERVATION_JOURS);
        try (Stream<Path> fichiers = Files.list(dossier)) {
            for (Path f : fichiers.toList()) {
                String nom = f.getFileName().toString();
                if (nom.startsWith("erreurs-") && nom.endsWith(".jsonl")) {
                    try {
                        if (LocalDate.parse(nom.substring(8, 18)).isBefore(limite)) {
                            Files.deleteIfExists(f);
                        }
                    } catch (RuntimeException ignoree) {
                        // nom inattendu : on n'y touche pas
                    }
                }
            }
        }
    }

    /** Une resolution : quand, par qui. Conservee dans erreurs/resolutions.json. */
    record Resolution(long quand, String par) {
    }

    private final Map<String, Resolution> resolutions = new java.util.HashMap<>();

    /**
     * Marque une cause d'erreur comme resolue : elle sort de la liste des erreurs actives, et y revient
     * (signalee "reapparue") si elle se produit de nouveau.
     */
    public synchronized boolean marquerResolu(String id, String par) {
        GroupeErreurs g = groupe(id);
        if (g == null) {
            return false;
        }
        long quand = Math.max(System.currentTimeMillis(), g.getDerniere());
        g.marquerResolu(quand, par);
        resolutions.put(g.getSignature(), new Resolution(quand, par));
        sauverResolutions();
        return true;
    }

    private void sauverResolutions() {
        if (dossier == null) {
            return;
        }
        try {
            json.writeValue(dossier.resolve("resolutions.json").toFile(), resolutions);
        } catch (IOException e) {
            System.err.println("Supervision : resolutions non enregistrees : " + e.getMessage());
        }
    }

    private void chargerResolutions() {
        Path fichier = dossier.resolve("resolutions.json");
        if (!Files.exists(fichier)) {
            return;
        }
        try {
            Map<String, Resolution> lues = json.readValue(fichier.toFile(),
                    json.getTypeFactory().constructMapType(java.util.HashMap.class, String.class, Resolution.class));
            resolutions.putAll(lues);
            for (Map.Entry<String, Resolution> r : lues.entrySet()) {
                GroupeErreurs g = groupes.get(r.getKey());
                if (g != null) {
                    g.marquerResolu(r.getValue().quand(), r.getValue().par());
                }
            }
        } catch (IOException e) {
            System.err.println("Supervision : resolutions illisibles : " + e.getMessage());
        }
    }

    /** Groupes, les plus recemment vus d'abord. */
    public synchronized List<GroupeErreurs> groupes() {
        List<GroupeErreurs> liste = new ArrayList<>(groupes.values());
        liste.sort(Comparator.comparingLong(GroupeErreurs::getDerniere).reversed());
        return liste;
    }

    public synchronized GroupeErreurs groupe(String id) {
        return groupes.values().stream().filter(g -> g.getId().equals(id)).findFirst().orElse(null);
    }

    /** Nombre d'erreurs survenues depuis {@code depuis} (millisecondes epoch), toutes causes confondues. */
    public synchronized long nombreDepuis(long depuis) {
        return horodatages.stream().filter(h -> h >= depuis).count();
    }

    public long getDemarrage() {
        return demarrage;
    }

    /** Pour les tests. */
    synchronized void vider() {
        groupes.clear();
        horodatages.clear();
    }

    private static String nn(String s) {
        return s == null ? "" : s;
    }
}
