# adminmobile — back-office mobile money (Spring Boot / Thymeleaf)

Back-office d'administration du mobile money de La Poste Togo (EcoCCP, e-poste, Flooz,
T-Money) : consultation des comptes et operations, etats de transactions, reclamations,
releves, parametrage des marchands, annulation d'operations.

Portage de l'application historique Java EE / JSF + PrimeFaces
(https://github.com/spring-sa0/adminmobile) vers **Spring Boot 3.5 / Thymeleaf**.

| | |
|---|---|
| Version | 1.4.0 |
| Java | 17 |
| Framework | Spring Boot 3.5.16 (Web, Security, Data JPA, Thymeleaf, Mail) |
| Base | Oracle (driver `ojdbc11`), schemas `SPT`, `IMOBILE`, `MBUDGET`, `EPOSTE`... |
| Etats / exports | JasperReports 6.21, Apache POI (Excel), OpenPDF (PDF), jsoup |

---

## Sommaire

1. [Demarrage rapide](#demarrage-rapide)
2. [Configuration](#configuration)
3. [Deploiement (Docker)](#deploiement-docker)
4. [Authentification et profils](#authentification-et-profils)
5. [Ecrans de recherche](#ecrans-de-recherche)
6. [Securite](#securite)
7. [Organisation du code](#organisation-du-code)
8. [Tests et integration continue](#tests-et-integration-continue)
9. [Points d'attention](#points-dattention)
10. [Backlog](#backlog)

---

## Demarrage rapide

Prerequis : JDK 17, Maven 3.9+, acces reseau a la base Oracle (port 1521).

```bash
cp .env.example .env        # puis renseigner les vraies valeurs (voir Configuration)
mvn spring-boot:run
```

L'application est disponible sur `http://localhost:8080/login` (ou le port defini par
`SERVER_PORT`).

Au lancement local (IDE ou `mvn`), le fichier `.env` a la racine est lu automatiquement
(`spring.config.import` dans `application.yml`). Les variables d'environnement du systeme
restent prioritaires sur ce fichier.

## Configuration

Toute la configuration passe par des variables d'environnement, lues depuis `.env` en local
ou via `env_file` en Docker. **Aucun identifiant ne doit etre ecrit dans le code ni dans
`application.yml`.** `.env` est exclu de git (`.gitignore`) et de l'image Docker
(`.dockerignore`) ; `.env.example` sert de modele.

| Variable | Obligatoire | Defaut | Role |
|---|---|---|---|
| `DB_URL` | oui | — | URL JDBC Oracle, ex. `jdbc:oracle:thin:@//hote:1521/SERVICE` |
| `DB_USERNAME` | oui | — | Compte Oracle technique de l'application (pool de connexions) |
| `DB_PASSWORD` | oui | — | Mot de passe de ce compte |
| `DB_POOL_SIZE` | non | `16` | Taille max du pool Hikari |
| `SERVER_PORT` | non | `8080` | Port HTTP |
| `THYMELEAF_CACHE` | non | `true` | Mettre `false` en developpement pour recharger les templates |
| `SHOW_SQL` | non | `false` | Journalise le SQL Hibernate (developpement uniquement) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | pour les mails | `localhost:25` | Serveur SMTP (notifications reclamations/suggestions) |
| `MAX_LOGIN_ATTEMPTS` | non | `5` | Echecs de connexion avant blocage d'un identifiant |
| `MAX_LOGIN_ATTEMPTS_PER_IP` | non | `20` | Echecs de connexion avant blocage d'une adresse IP |
| `LOGIN_LOCK_DURATION` | non | `15m` | Duree du blocage |
| `RECHERCHE_PERIODE_MAX_JOURS` | non | `92` | Duree maximale d'une periode de recherche |
| `RECHERCHE_TAILLE_PAGE` | non | `100` | Lignes par page dans les listes de resultats |
| `LOG_DIR` | non | `logs` (`/app/logs` en Docker) | Dossier des journaux |
| `EXPORT_MAX_LIGNES_EXCEL` | non | `50000` | Lignes maximum par export Excel d'une liste |
| `EXPORT_MAX_LIGNES_PDF` | non | `5000` | Lignes maximum par export PDF d'une liste |

## Deploiement (Docker)

Le `Dockerfile` construit l'application en deux etapes (build Maven, puis image JRE 17
executee sous un utilisateur non-root). Ni Java ni Maven ne sont necessaires sur le serveur.

```bash
git clone https://github.com/spring-sa0/adminmobile-springboot.git
cd adminmobile-springboot
cp .env.example .env && chmod 600 .env    # renseigner les valeurs
docker compose up -d --build
```

Verification :

```bash
docker compose ps              # etat "healthy" apres ~1 min
docker compose logs -f app     # attendre "Started AdminMobileApplication"
```

Le healthcheck interroge `/actuator/health`, qui verifie aussi la connexion a Oracle : le
conteneur passe `unhealthy` si la base est injoignable. Cette URL est publique mais ne renvoie
que `{"status":"UP"}` ou `DOWN`, utilisable aussi par un outil de supervision externe.

### Journaux

Les journaux sont ecrits dans le volume Docker `logs` (`/app/logs` dans le conteneur),
conserve quand le conteneur est recree :

| Fichier | Contenu | Conservation |
|---|---|---|
| `adminmobile.log` | Journal technique : erreurs, references des pages d'erreur 500 | 30 jours |
| `audit.log` | Connexions, echecs, blocages, deconnexions et **toutes les actions** (qui, IP, ecran, parametres, resultat) | 1 an |

Rotation quotidienne (et a 50 Mo), anciens fichiers compresses.

```bash
docker compose exec app tail -f /app/logs/audit.log
docker compose exec app grep "Erreur A3F9C21B" /app/logs/adminmobile.log   # reference d'une page 500
```

Exemple de ligne d'audit :

```
2026-10-15 10:12:41.208 ACTION utilisateur=AGENT1 ip=10.0.0.12 action="POST /annulation-op/annuler" numop=27198715 resultat="OK: Operation annulee."
```

Les mots de passe et le jeton CSRF n'y figurent jamais (`***`).

Port expose : celui de `SERVER_PORT` s'il est defini dans `.env`, sinon **8099** cote hote
(redirige vers 8080 dans le conteneur).

Mise a jour :

```bash
git pull
docker compose up -d --build
```

Options JVM (memoire...) : variable `JAVA_OPTS`, ex. `JAVA_OPTS=-Xmx512m`.

### HTTPS

L'application sert du HTTP simple : **en production, placez-la derriere un reverse proxy
HTTPS** (Nginx, Caddy...) et n'exposez pas son port directement. Les logins sont des
identifiants Oracle, ils ne doivent pas circuler en clair.
L'application prend en compte les en-tetes `X-Forwarded-*` du proxy
(`server.forward-headers-strategy: native`) : le cookie de session est alors marque `Secure`
et l'IP reelle du client sert au blocage des tentatives de connexion.

## Authentification et profils

L'authentification reprend a l'identique le mecanisme de l'application d'origine
(`security/LegacyDbAuthenticationProvider.java`) :

1. le login et le mot de passe saisis servent a **ouvrir une connexion Oracle** ;
2. si elle reussit, le profil est lu dans `MBUDGET.UTILISATEUR_LOG.CODE_PROFIL`.

**Chaque utilisateur du back-office doit donc avoir un compte Oracle**, et c'est son mot de
passe Oracle qu'il saisit. Le compte technique de l'application (`DB_USERNAME`) est refuse a
la connexion.

| Profil (`CODE_PROFIL`) | Role | Acces |
|---|---|---|
| 1 | Administrateur | Tout, dont Parametres, Annulation d'operations, Gateway, Types de reclamation |
| 2 | Utilisateur | Reclamations, suggestions, consultations, operations, comptes, etats, e-poste, commissions, marchand |
| 3 | Consultation | Releves, marchand |
| 9 | Marchand | Marchand uniquement |

Les regles d'acces sont definies dans `security/SecurityConfig.java` et correspondent a
l'affichage du menu (`templates/fragments/menu.html`) : un ecran absent du menu d'un profil
lui est aussi refuse par URL.

## Ecrans de recherche

Regles communes a tous les ecrans de liste, sans code specifique dans chaque controleur :

- **Periode limitee** a 92 jours (`RECHERCHE_PERIODE_MAX_JOURS`) : les operations augmentent
  d'environ 1 000 lignes par jour, une recherche sur plusieurs annees chargerait des centaines
  de milliers de lignes. Controle dans `WebDateUtils.erreurPeriode`.
- **Pagination** a 100 lignes (`RECHERCHE_TAILLE_PAGE`) de toute liste exposee sous le nom
  `results` (`web/PaginationInterceptor.java`, barre `fragments/pagination.html` a inclure
  apres le tableau). La pagination se fait a l'affichage : c'est la limite de periode qui borne
  le volume lu en base.
- **Telephones et numeros de compte** : mis en forme automatiquement (`+228 90 12 34 56`,
  `22890123456` -> `90123456` ; espaces retires des comptes) par
  `web/SaisieNormalisationFilter.java`, puis une recherche avec une valeur invalide est
  refusee avec un message clair (`web/SaisieValidationInterceptor.java`).

- **Export Excel / PDF** de toute liste affichee (boutons au-dessus du tableau). Le fichier
  contient **toute la liste**, pas seulement la page affichee, avec les memes colonnes que
  l'ecran, le titre, les criteres de recherche, la date et l'auteur de l'export. Plafonds :
  50 000 lignes en Excel, 5 000 en PDF (`EXPORT_MAX_LIGNES_EXCEL` / `EXPORT_MAX_LIGNES_PDF`) ;
  au-dela, la page s'affiche avec un message. Chaque export est trace dans `audit.log`
  (evenement `EXPORT`). Les regles d'acces de l'ecran s'appliquent a son export.

  Fonctionnement (`web/ExportFilter.java`) : la page est rendue normalement avec
  `?export=xlsx|pdf`, sans pagination, puis son tableau `class="data-table"` est converti en
  fichier (`export/ExtracteurTableau.java`, `ExportExcel`, `ExportPdf`). Tout nouvel ecran est
  donc exportable sans code supplementaire : il suffit d'inclure
  `<div th:replace="~{fragments/export :: boutons}"></div>` avant le tableau (verifie par les
  tests). Les colonnes d'action (boutons, formulaires) ou a en-tete vide ne sont pas exportees ;
  la classe `no-export` exclut une colonne ou un tableau.

Formats en base, a connaitre pour toute nouvelle requete : `USERS_EPOSTE.TELEPHONE` et
`SPT.COMPTE_APP_LIER.TELEPHONE_CLIENT` sont sur **8 chiffres**,
`SPT.RECU_ELECTRONIQUE.TELEPHONE` porte l'**indicatif** (`228` + 8 chiffres). Toute jointure
entre ces tables doit ajouter `228` cote comptes.

## Securite

Mesures en place (version 1.1.0) :

- **Blocage des tentatives de connexion** : apres 5 echecs pour un identifiant ou 20 pour une
  IP, blocage de 15 minutes, verifie *avant* tout appel a Oracle
  (`security/LoginAttemptService.java`). Cela evite le brute force et le verrouillage des
  comptes Oracle par le profil de la base. Le compteur est en memoire : il repart a zero au
  redemarrage et n'est pas partage entre plusieurs instances.
- **Format d'identifiant controle** (identifiant Oracle simple) : refuse les syntaxes
  speciales du driver (`sys as sysdba`, connexion proxy `user[cible]`).
- **CSRF** actif sur tous les formulaires.
- **En-tetes HTTP** : `Content-Security-Policy`, `X-Frame-Options: DENY`,
  `X-Content-Type-Options`, `Referrer-Policy`, `Cache-Control: no-store`.
  La CSP autorise `'unsafe-inline'` pour les scripts car les templates contiennent des
  scripts et des `onclick` en ligne ; elle bloque malgre tout les scripts externes, l'envoi de
  donnees vers un autre domaine et l'affichage dans une iframe.
- **Cookie de session** `HttpOnly`, `SameSite=Lax`, `Secure` derriere un proxy HTTPS ;
  session limitee a 30 min d'inactivite et a une session simultanee par utilisateur.
- **Pages d'erreur** sans trace, message d'exception ni SQL : `templates/error/404.html`,
  `templates/error/5xx.html` (toutes les erreurs serveur) et `templates/error.html` (autres
  codes : 403, 400...). Chaque erreur serveur recoit une reference affichee a l'utilisateur et
  journalisee avec l'exception (`web/ErrorReferenceAttributes.java`) : rechercher
  `Erreur <reference>` dans les logs pour en retrouver la cause.
- **Formulaires** : seuls les champs affiches sont liables (protection contre le
  *mass assignment*).
- **SQL** entierement parametre (JPQL, `JdbcTemplate`, rapports Jasper en `$P{}`).

### Identifiants exposes dans l'historique git

Un ancien mot de passe Oracle figure dans l'historique git de ce depot (versions anterieures
de `application.yml`) et dans celui du depot legacy. **Ce mot de passe doit etre change cote
Oracle** : le retirer des fichiers actuels ne suffit pas, l'historique reste lisible par
toute personne ayant acces aux depots.

## Organisation du code

```
src/main/java/com/laposte/adminmobile/
├── security/     authentification Oracle, blocage des tentatives, regles d'acces, audit
├── web/          controllers Spring MVC, pagination, controle des saisies, pages d'erreur
├── service/      logique metier, appels aux procedures stockees, SMS, mails, Jasper
├── repository/   acces aux donnees (Spring Data JPA, requetes natives, JdbcTemplate)
├── entities/     entites JPA (portees du legacy)
├── export/       export Excel / PDF des listes affichees
└── util/         utilitaires (generation de mots de passe)

src/main/resources/
├── application.yml
├── templates/    pages Thymeleaf (fragments/ : head, menu, footer)
├── static/       CSS, JS, images
└── reports/      definitions JasperReports (.jrxml)
```

| Domaine | Controllers |
|---|---|
| Authentification | `LoginController`, `security/*` |
| Accueil / menu | `HomeController`, `fragments/menu.html` |
| Comptes | `ComptesController`, `CompteLieController` |
| Operations / transactions | `OperationConsultationController`, `TransactionEtatController`, `AnnulationOpController`, `GatewayController`, `EposteOperationController` |
| Reclamations / suggestions / support | `ReclamationController`, `TypeReclamationController`, `SuggestionController`, `SupportController` |
| Releves + JasperReports | `ReleveController`, `ReleveExportController`, `JasperReportService` |
| Marchand / commissions / recap | `MarchandController`, `MarchandParametrageController`, `CommissionsController`, `RecapController`, `EposteReportController`, `SuggestionsReportController` |
| Parametres (profil, mot de passe, e-mail, activation) | `ProfilController`, `PasswordResetController`, `EmailUpdateController`, `CompteActivationController` |

Procedures stockees appelees : `PROCEDURES.ANNUL_OP_ECOCCP(numop, utilisateur)` et
`PROCEDURES.RESTAURER_OP_ECOCCP(numop, utilisateur)` (annulation / restauration
d'operations), plus les procedures de preparation des releves (`ReleveExportService`).
Les SMS sont envoyes en inserant dans `IMOBILE.INFO_SMS`, lue par la passerelle SMS.

## Tests et integration continue

```bash
mvn test
```

- `AllPagesTest` : affiche chaque ecran avec des services simules (sans base), execute les
  actions POST, et verifie les regles d'acces par profil, les en-tetes de securite, la
  protection des formulaires, les pages d'erreur, la pagination, la limite de periode et le
  controle des saisies.
- `LoginAttemptServiceTest` : blocage des tentatives et refus des identifiants interdits sans
  connexion a Oracle.
- `SaisiesEtPaginationTest`, `AuditTest` : regles de saisie, calcul des pages, nettoyage des
  lignes d'audit.
- `JasperFontsTest` : toute police citee dans un `.jrxml` doit etre embarquee
  (`fonts/fonts.xml` : Arial et Times New Roman sont fournies par Liberation Sans / Serif, de
  memes metriques, car absentes sous Linux), et les rapports se remplissent et s'exportent
  sans base.

Les tests ne touchent pas la base : les requetes SQL elles-memes sont validees au demarrage
de l'application (requetes JPQL) et par l'usage sur une base de recette.

**CI** (`.github/workflows/ci.yml`) : a chaque push et pull request, GitHub Actions lance
`mvn verify` puis construit l'image Docker. Une PR qui casse un test est signalee avant la
fusion.

## Points d'attention

1. **Annulation d'operations = ecritures reelles.** `/annulation-op/annuler` appelle
   `PROCEDURES.ANNUL_OP_ECOCCP`, qui passe des contre-ecritures comptables (`SPT.MOUVEMENT`),
   deplace l'operation dans `SPT.RECU_ELECTRONIQUE_ANNULE` et envoie un SMS au client. Ne pas
   tester cet ecran sur la production avec une operation qu'on ne veut pas annuler. La
   procedure n'enregistre aucun motif d'annulation.

2. **Ecrans "Etats" reconstruits.** Plusieurs rapports legacy ("Etats", "Marchand",
   "Commissions", "Recap") n'existaient plus que sous forme de `.jasper` compiles sans leur SQL
   source. Leurs requetes ont ete reconstruites par deduction ; chaque cas est signale par un
   `TODO` dans le controller ou le template concerne (`grep -rn TODO src/main`, une
   cinquantaine d'occurrences). `/eposte/chequiers` et `/eposte/achats-credit` s'appuient sur
   des tables (`CHEQUIER`, `ACHAT_CREDIT`) absentes du code legacy : schema plausible mais
   **non confirme**. A valider avec le metier sur une base de recette.

3. **`/parametres/modif-password` a une portee limitee.** L'ecran modifie `CODE_SECRET` dans
   `USERS_EPOSTE` (comptes clients mobile), comme l'original, et envoie le nouveau mot de passe
   par SMS. Il ne change pas le mot de passe Oracle utilise pour se connecter au back-office.
   Ces mots de passe sont stockes en clair dans la base (heritage du legacy).

4. **Journal `MODIF_CODESECRET`.** Cette table d'audit n'a pas de vraie cle : `TELEPHONE`,
   mappe en `@Id`, y apparait plusieurs fois. L'entite n'est donc utilisee qu'en insertion
   (`persist`) et en lecture par expression constructeur ; ne jamais utiliser
   `findById`/`save` dessus (voir `ModifCodesecret.java`).

5. **Doublon `Users` / `UsersEposte`.** Les deux entites mappent la meme table
   `USERS_EPOSTE` (deja le cas dans le legacy). `Users` sert pour le profil, le mot de passe,
   l'e-mail et l'activation. A terme, les fusionner pour eviter des incoherences de cache JPA.

6. **Ecrans Marchand ouverts a tous les profils.** Tous les profils portent l'autorisation
   `MARCHAND` (comme dans le legacy) et peuvent donc parametrer, activer et desactiver des
   marchands, y compris le profil Consultation. A restreindre si ce n'est pas voulu.

7. **Exports de releves simultanes.** Les procedures `IMOBILE.procedure_solde_tgcel`,
   `procedure_solde_tgcel2` et `gen_afb` vident puis remplissent des tables de travail
   partagees, que l'etat Jasper relit ensuite. Si deux utilisateurs exportent en meme temps,
   l'un peut recuperer les lignes de l'autre (ou un fichier vide). La correction passe par les
   procedures PL/SQL (tables indexees par session, par exemple) ; voir
   `ReleveExportService.java`.

8. **Authentification par comptes Oracle.** Fidele au legacy mais fortement couplee a la
   base. A moyen terme, une table d'utilisateurs applicative avec mots de passe haches
   (BCrypt) serait preferable.

## Backlog

| Ecran legacy | Raison | Etat |
|---|---|---|
| `About.xhtml` | Page d'information statique | Non porte (faible priorite) |
| `chargerFichier.xhtml` | Widget d'upload generique, usage a confirmer | A investiguer |
| `listereleve.xhtml` | Plus lie depuis le menu, probablement du code mort | A confirmer avec le metier |

Suites recommandees :

1. Changer le mot de passe Oracle expose dans l'historique git.
2. Mettre en place le reverse proxy HTTPS en production.
3. Traiter les `TODO` des ecrans reconstruits avec l'equipe metier, sur une base de recette.
4. Decider du perimetre des ecrans Marchand (point 6) et de l'evolution de l'authentification
   (point 8).
5. Isoler les tables de travail des exports de releves par session (point 7).
