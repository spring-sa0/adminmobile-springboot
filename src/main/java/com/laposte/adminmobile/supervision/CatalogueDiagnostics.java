package com.laposte.adminmobile.supervision;

import com.laposte.adminmobile.supervision.Diagnostic.Gravite;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Catalogue des causes d'erreur connues de l'application et de leur resolution.
 * <p>
 * Chaque regle reconnait une erreur a partir du texte complet de l'evenement (message journalise,
 * classes et messages de toute la chaine d'exceptions). La premiere regle qui correspond
 * l'emporte : les regles les plus precises sont donc en tete. Une erreur non reconnue recoit un
 * diagnostic generique qui indique ou chercher.
 * <p>
 * Pour ajouter une cause : une entree {@link #regle} avec un motif sur le code Oracle ou la classe
 * d'exception, et des actions concretes (quoi verifier, quelle variable de .env, quelle commande).
 */
public final class CatalogueDiagnostics {

    private record Regle(Pattern motif, Diagnostic diagnostic) {
    }

    private static final String BASE = "Base de donnees Oracle";
    private static final String CONFIG = "Configuration";
    private static final String CODE = "Code / schema";
    private static final String CHARGE = "Charge / ressources";
    private static final String EXTERNE = "Service externe";

    private static final List<Regle> REGLES = List.of(
            // ---- Compte Oracle technique et connexion a la base ---------------------------------
            regle("ORA-28001|ORA-28011|password (has )?expired|mot de passe a expir",
                    "ORACLE_MOT_DE_PASSE_EXPIRE", "Mot de passe Oracle du compte technique expire", Gravite.CRITIQUE, BASE,
                    "Le mot de passe du compte de l'application (DB_USERNAME) a expire. Pendant la periode de grace "
                            + "les connexions marchent encore ; a la fin, plus aucune connexion n'est possible et toute "
                            + "l'application s'arrete.",
                    "Demander au DBA de changer le mot de passe du compte DB_USERNAME (ALTER USER ... IDENTIFIED BY ...).",
                    "Mettre a jour DB_PASSWORD dans le fichier .env du serveur.",
                    "Redemarrer l'application : docker compose up -d.",
                    "Demander au DBA un profil sans expiration (PASSWORD_LIFE_TIME UNLIMITED) pour ce compte technique, "
                            + "ou planifier le renouvellement avant chaque echeance (la supervision previent par e-mail)."),
            regle("ORA-28000|account is locked|compte est verrouill",
                    "ORACLE_COMPTE_VERROUILLE", "Compte Oracle verrouille", Gravite.CRITIQUE, BASE,
                    "Oracle a verrouille le compte apres trop d'echecs de connexion (FAILED_LOGIN_ATTEMPTS du profil). "
                            + "S'il s'agit du compte technique, toute l'application est coupee ; s'il s'agit d'un agent, "
                            + "lui seul ne peut plus se connecter.",
                    "Identifier le compte dans le detail de l'erreur ou dans audit.log (CONNEXION_ECHEC).",
                    "Demander au DBA de le deverrouiller : ALTER USER <compte> ACCOUNT UNLOCK.",
                    "Si c'est le compte technique : verifier DB_PASSWORD dans .env (un mot de passe errone le verrouille).",
                    "Verifier que MAX_LOGIN_ATTEMPTS reste inferieur au FAILED_LOGIN_ATTEMPTS du profil Oracle (3)."),
            regle("ORA-01017|invalid username/password|nom utilisateur/mot de passe non valide",
                    "ORACLE_IDENTIFIANTS_INVALIDES", "Identifiants Oracle refuses", Gravite.CRITIQUE, BASE,
                    "La base refuse le compte ou le mot de passe de l'application (DB_USERNAME / DB_PASSWORD).",
                    "Verifier DB_USERNAME et DB_PASSWORD dans .env (sans espace ni guillemet en trop).",
                    "Verifier aupres du DBA que le mot de passe n'a pas ete change.",
                    "Redemarrer l'application apres correction : docker compose up -d."),
            regle("ORA-125(14|05|28)|ORA-12541|ORA-12170|ORA-12543|ORA-03113|ORA-03114|ORA-17002|IO Error|Network Adapter could not establish"
                            + "|Connection refused|connect timed out|Got minus one from a read call|No more data to read from socket",
                    "ORACLE_INJOIGNABLE", "Base Oracle injoignable ou connexion coupee", Gravite.CRITIQUE, BASE,
                    "Le serveur ne peut pas joindre la base (reseau, pare-feu, base ou listener arrete), ou la "
                            + "connexion a ete coupee en cours de route.",
                    "Tester depuis le serveur : nc -zv <hote-oracle> 1521.",
                    "Verifier DB_URL dans .env (hote, port, nom de service).",
                    "Contacter le DBA : base demarree ? listener actif (lsnrctl status) ?",
                    "Si l'erreur est isolee apres une periode calme : verifier que max-lifetime du pool reste sous "
                            + "l'IDLE_TIME du profil Oracle (30 min)."),
            regle("ORA-00018|ORA-00020|ORA-12516|ORA-12519|maximum number of (sessions|processes)",
                    "ORACLE_SESSIONS_EPUISEES", "Plus de sessions disponibles sur la base", Gravite.CRITIQUE, BASE,
                    "L'instance Oracle a atteint sa limite de sessions ou de processus (toutes applications confondues).",
                    "Demander au DBA l'occupation des sessions (v$session) et les applications qui les consomment.",
                    "Verifier que DB_POOL_SIZE n'a pas ete augmente excessivement.",
                    "Le DBA peut augmenter les parametres sessions / processes si necessaire."),
            regle("Connection is not available, request timed out|SQLTransientConnectionException|Unable to acquire JDBC Connection",
                    "POOL_SATURE", "Pool de connexions Oracle sature", Gravite.ELEVEE, CHARGE,
                    "Toutes les connexions du pool (DB_POOL_SIZE) etaient occupees pendant plus de 30 s : requetes "
                            + "lentes, trop de traitements simultanes, ou base qui ralentit.",
                    "Regarder dans la page de supervision l'occupation du pool et la latence Oracle.",
                    "Chercher les ecrans lents (journal technique, temps de reponse) : gros etats, exports.",
                    "Abaisser CHARGE_TRAITEMENTS_LOURDS ou augmenter DB_POOL_SIZE (avec l'accord du DBA).",
                    "Si la latence Oracle est elevee : voir avec le DBA (verrous, statistiques, charge de la base)."),
            // ---- Ecarts entre le code et le schema / les droits ----------------------------------
            regle("ORA-00904|invalid identifier|identificateur non valide",
                    "SCHEMA_COLONNE_INEXISTANTE", "Colonne inexistante en base", Gravite.ELEVEE, CODE,
                    "Une requete utilise une colonne qui n'existe pas dans la table : le code (entite JPA ou requete) "
                            + "ne correspond pas au schema de la base, ou la colonne a ete renommee / supprimee.",
                    "Lire le nom de la colonne dans le message (ex. \"RE1_0\".\"MOTIF_ANNULATION\").",
                    "Comparer avec la base : SELECT column_name FROM all_tab_columns WHERE table_name = '<TABLE>'.",
                    "Corriger le mapping de l'entite ou la requete, puis redeployer."),
            regle("ORA-00942|table or view does not exist|table ou vue inexistante",
                    "SCHEMA_TABLE_INEXISTANTE", "Table ou vue introuvable", Gravite.ELEVEE, CODE,
                    "La table n'existe pas, ou le compte technique n'a pas le droit de la lire (Oracle renvoie la meme erreur).",
                    "Identifier la table dans la requete du detail de l'erreur.",
                    "Verifier son existence et son schema avec le DBA.",
                    "Demander le droit necessaire : GRANT SELECT ON <schema>.<table> TO <compte technique>."),
            regle("ORA-01031|insufficient privileges|privileges insuffisants",
                    "DROITS_INSUFFISANTS", "Droits Oracle insuffisants", Gravite.ELEVEE, BASE,
                    "Le compte technique n'a pas le droit d'executer l'operation (table, procedure, sequence).",
                    "Identifier l'objet vise dans le detail de l'erreur.",
                    "Demander au DBA le GRANT correspondant (SELECT, INSERT, EXECUTE...) au compte DB_USERNAME."),
            regle("PLS-00306|PLS-00201|ORA-06550|wrong number or types of arguments|must be declared",
                    "PROCEDURE_SIGNATURE", "Appel de procedure stockee invalide", Gravite.ELEVEE, CODE,
                    "La procedure appelee n'existe pas ou n'est plus accessible, ou sa signature (nombre / type des "
                            + "parametres) a change en base.",
                    "Lire le nom de la procedure dans le detail de l'erreur.",
                    "Comparer sa signature : SELECT * FROM all_arguments WHERE object_name = '<PROCEDURE>'.",
                    "Aligner l'appel Java (service concerne) sur la signature reelle, puis redeployer."),
            regle("ORA-04068|ORA-04061|ORA-04065|existing state of packages has been discarded",
                    "PROCEDURE_INVALIDE", "Procedure ou package Oracle recompile / invalide", Gravite.MOYENNE, BASE,
                    "Un package ou une procedure a ete modifie ou recompile en base pendant que l'application l'utilisait.",
                    "Relancer l'operation : l'erreur disparait souvent au second appel.",
                    "Si elle persiste : demander au DBA de verifier les objets invalides (all_objects WHERE status = 'INVALID')."),
            regle("ORA-00001|unique constraint|contrainte unique",
                    "DOUBLON", "Doublon refuse par la base", Gravite.MOYENNE, CODE,
                    "L'enregistrement existe deja (contrainte d'unicite), souvent un double clic ou une saisie deja faite.",
                    "Verifier si la donnee existe deja (nom de la contrainte dans le message).",
                    "Si l'erreur se repete sur un ecran : ajouter un controle d'existence avant l'enregistrement."),
            regle("ORA-01400|cannot insert NULL|impossible d'inserer NULL",
                    "VALEUR_OBLIGATOIRE", "Champ obligatoire vide en base", Gravite.MOYENNE, CODE,
                    "Une colonne NOT NULL recoit une valeur vide : un champ du formulaire n'est pas renseigne ou pas transmis.",
                    "Lire la colonne dans le message (\"SCHEMA\".\"TABLE\".\"COLONNE\").",
                    "Ajouter le controle de saisie correspondant dans l'ecran concerne."),
            regle("ORA-12899|value too large|valeur trop grande",
                    "VALEUR_TROP_LONGUE", "Saisie trop longue pour la colonne", Gravite.MOYENNE, CODE,
                    "Une saisie depasse la taille de la colonne (le message donne la taille reelle et la taille maximale).",
                    "Ajouter une limite de longueur sur le champ du formulaire (maxlength et controle serveur)."),
            regle("ORA-01722|invalid number|nombre non valide|NumberFormatException",
                    "NOMBRE_INVALIDE", "Valeur non numerique", Gravite.MOYENNE, CODE,
                    "Une valeur attendue numerique contient autre chose (saisie, ou donnee incoherente en base).",
                    "Identifier l'ecran (URL) et la saisie en cause dans les occurrences.",
                    "Ajouter un controle de saisie, ou filtrer les donnees non numeriques dans la requete."),
            regle("ORA-01013|user requested cancel|ORA-01555|snapshot too old|QueryTimeoutException",
                    "REQUETE_TROP_LONGUE", "Requete interrompue ou trop longue", Gravite.MOYENNE, CHARGE,
                    "Une requete a ete annulee ou a dure trop longtemps (gros volume, base chargee).",
                    "Identifier l'ecran dans les occurrences et la periode demandee.",
                    "Reduire la periode de recherche, ou faire analyser la requete par le DBA (index, statistiques)."),
            regle("Duplicate row was found and .ASSERT. was specified|NonUniqueResultException|query did not return a unique result",
                    "IDENTIFIANT_NON_UNIQUE", "Identifiant d'entite non unique en base", Gravite.ELEVEE, CODE,
                    "Une entite JPA est mappee sur une colonne qui n'est pas unique en base (ex. journal dont "
                            + "TELEPHONE sert d'identifiant). Hibernate trouve plusieurs lignes la ou il en attend une.",
                    "Identifier l'entite dans la trace (repository ou service appelant).",
                    "Ne pas utiliser findById / save (merge) sur cette entite : insertion par persist, lecture par requete dediee.",
                    "A terme : mapper une vraie cle unique."),
            // ---- Ressources --------------------------------------------------------------------
            regle("OutOfMemoryError|Java heap space|GC overhead limit exceeded",
                    "MEMOIRE_SATUREE", "Memoire Java saturee", Gravite.CRITIQUE, CHARGE,
                    "L'application n'a plus de memoire : trop de gros etats ou exports en meme temps, ou memoire allouee "
                            + "trop faible. La JVM s'arrete et Docker la redemarre (sessions perdues).",
                    "Verifier la RAM du serveur et la memoire allouee (JAVA_OPTS, 75 % de la RAM du conteneur par defaut).",
                    "Abaisser CHARGE_TRAITEMENTS_LOURDS et / ou EXPORT_MAX_LIGNES_EXCEL.",
                    "Regarder les occurrences : quels ecrans tournaient au moment de la saturation ?"),
            regle("No space left on device|Espace insuffisant|disk full",
                    "DISQUE_PLEIN", "Disque plein", Gravite.CRITIQUE, CHARGE,
                    "Le disque du serveur est plein : journaux, exports temporaires ou images Docker.",
                    "Verifier l'espace : df -h, et docker system df.",
                    "Supprimer les images Docker inutiles : docker image prune.",
                    "Verifier la taille du volume des journaux (logs) : la rotation garde 30 jours (1 an pour l'audit)."),
            // ---- Exports / rapports ------------------------------------------------------------
            regle("JRFontNotFoundException|Font \".*\" is not available",
                    "POLICE_ABSENTE", "Police manquante pour un rapport Jasper", Gravite.ELEVEE, CONFIG,
                    "Un rapport utilise une police absente du serveur et non embarquee dans l'application.",
                    "Ajouter la police dans fonts/fonts.xml (police libre de memes metriques, comme Liberation pour Arial).",
                    "Lancer JasperFontsTest pour verifier, puis redeployer."),
            regle("JRExceptionWrapper|JRException|Impossible de (compiler|generer|exporter) le rapport",
                    "RAPPORT_JASPER", "Echec de generation d'un rapport", Gravite.ELEVEE, CODE,
                    "La generation d'un etat Jasper a echoue (requete du rapport, donnees, police ou modele .jrxml).",
                    "Lire la cause en fin de chaine d'exceptions dans le detail de l'erreur.",
                    "Verifier les tables de travail remplies par la procedure du releve (vides ?).",
                    "Tester le rapport avec le numero de compte en cause."),
            // ---- Services externes -------------------------------------------------------------
            regle("AddressException|Illegal address|MailParseException",
                    "MAIL_ADRESSE_INVALIDE", "Adresse e-mail invalide (expediteur ou destinataire)", Gravite.MOYENNE, CONFIG,
                    "Une adresse e-mail est vide ou mal formee. Cas le plus frequent : expediteur vide (\"Illegal address "
                            + "in string ''\") ; ou destinataire mal saisi (alertes, notifications).",
                    "Expediteur : renseigner MAIL_FROM dans .env (sinon MAIL_USERNAME s'il s'agit d'une adresse).",
                    "Destinataires des alertes : verifier SUPERVISION_ALERTES_EMAILS (adresses separees par des virgules).",
                    "Verifier aussi que MAIL_HOST est renseigne : sans serveur SMTP, aucun e-mail ne part (indicateur "
                            + "\"E-mails\" de la supervision).",
                    "Redemarrer l'application apres modification de .env."),
            regle("STARTTLS",
                    "MAIL_STARTTLS", "Le serveur de messagerie exige une connexion chiffree (STARTTLS)", Gravite.MOYENNE, CONFIG,
                    "Le serveur SMTP (port 587 en general) refuse l'envoi tant que la connexion n'est pas chiffree : "
                            + "\"530 Must issue a STARTTLS command first\".",
                    "Verifier MAIL_STARTTLS=true dans .env (valeur par defaut).",
                    "Verifier MAIL_PORT (587 pour la soumission chiffree).",
                    "Redemarrer l'application apres modification de .env."),
            regle("553|554|550 5\\.7\\.1|Sender address rejected|not owned by user|not authorized to send|Client does not have permissions to send as",
                    "MAIL_EXPEDITEUR_REFUSE", "Expediteur refuse par le serveur de messagerie", Gravite.MOYENNE, CONFIG,
                    "Le serveur refuse d'envoyer au nom de l'adresse expeditrice : elle ne correspond pas au compte SMTP "
                            + "authentifie (MAIL_USERNAME).",
                    "Renseigner MAIL_FROM dans .env avec l'adresse du compte SMTP (ex. <MAIL_USERNAME>@laposte.tg).",
                    "Ou demander a la DSI le droit d'envoyer au nom de l'expediteur voulu (ex. sms.contact@laposte.tg).",
                    "Redemarrer l'application apres modification de .env."),
            regle("AuthenticationFailedException|MailAuthenticationException|535 |authentication failed",
                    "MAIL_AUTHENTIFICATION", "Identifiants SMTP refuses", Gravite.MOYENNE, CONFIG,
                    "Le serveur de messagerie refuse MAIL_USERNAME / MAIL_PASSWORD (erreur, mot de passe change ou expire).",
                    "Verifier MAIL_USERNAME et MAIL_PASSWORD dans .env aupres de la DSI.",
                    "Si le serveur n'exige pas d'authentification (relais interne) : MAIL_AUTH=false.",
                    "Redemarrer l'application apres modification de .env."),
            regle("MailSendException|MailAuthenticationException|MailException|MailConnectException|Couldn't connect to host"
                            + "|SMTP|javax?\\.mail|jakarta\\.mail",
                    "MAIL_ECHEC", "Envoi d'e-mail impossible", Gravite.MOYENNE, EXTERNE,
                    "Le serveur de messagerie refuse ou ne repond pas (adresse, port, identifiants, pare-feu).",
                    "Verifier MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD dans .env.",
                    "Tester depuis le serveur : nc -zv <MAIL_HOST> <MAIL_PORT>.",
                    "Les operations metier continuent : seules les notifications (et alertes de supervision) sont perdues."),
            // ---- Sans impact -------------------------------------------------------------------
            regle("ClientAbortException|Broken pipe|Connection reset by peer|AsyncRequestNotUsableException|connexion .* interrompue",
                    "CLIENT_DECONNECTE", "Navigateur ferme pendant la reponse", Gravite.FAIBLE, "Reseau",
                    "L'utilisateur a ferme la page ou perdu le reseau avant la fin de la reponse (souvent un gros export).",
                    "Aucune action si c'est occasionnel.",
                    "Si c'est frequent sur les exports : reduire les plafonds d'export ou la periode de recherche.")
    );

    private CatalogueDiagnostics() {
    }

    private static Regle regle(String motif, String code, String titre, Gravite gravite, String categorie,
                               String cause, String... actions) {
        return new Regle(Pattern.compile(motif, Pattern.CASE_INSENSITIVE),
                new Diagnostic(code, titre, gravite, categorie, cause, List.of(actions)));
    }

    /** Diagnostic de l'erreur decrite par {@code texte} (message + chaine d'exceptions). */
    public static Diagnostic diagnostiquer(String texte) {
        String t = texte == null ? "" : texte;
        for (Regle r : REGLES) {
            if (r.motif().matcher(t).find()) {
                return r.diagnostic();
            }
        }
        return new Diagnostic("NON_REPERTORIEE", "Erreur non repertoriee", Gravite.MOYENNE, "Autre",
                "Cette erreur ne correspond a aucune cause connue du catalogue.",
                List.of("Lire la cause racine (derniere exception de la chaine) et la premiere ligne de code "
                                + "de l'application (com.laposte...) dans la trace.",
                        "Retrouver le contexte : URL, utilisateur, heure, et la reference montree a l'utilisateur.",
                        "Chercher la meme reference dans le journal technique : grep \"Erreur <reference>\" adminmobile.log.",
                        "Une fois la cause connue, l'ajouter au catalogue (supervision/CatalogueDiagnostics.java)."));
    }

    /** Toutes les regles du catalogue, pour la page d'aide de la supervision. */
    public static List<Diagnostic> regles() {
        return REGLES.stream().map(Regle::diagnostic).toList();
    }
}
