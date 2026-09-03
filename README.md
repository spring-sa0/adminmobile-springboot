# adminmobile — portage Spring Boot / Thymeleaf

Portage du back-office mobile money de La Poste Togo (EcoCCP, e-poste, Flooz, T-Money),
historiquement en Java EE / JSF + PrimeFaces, vers Spring Boot 3 / Thymeleaf.

Source legacy portee : https://github.com/spring-sa0/adminmobile

## ⚠️ A LIRE AVANT TOUTE MISE EN PRODUCTION

1. **Identifiants exposes dans le repo d'origine.** `web/WEB-INF/glassfish-resources.xml`
   du projet legacy contient un utilisateur/mot de passe Oracle en clair et une adresse de
   serveur qui ressemble a une prod reelle (`orcprod.laposte.tg`). Le repo GitHub source est
   public. **Faites tourner ces identifiants cote Oracle immediatement**, independamment de
   ce portage. Ce nouveau projet ne reprend AUCUN identifiant en dur : tout passe par des
   variables d'environnement (voir `application.yml`).

2. **Architecture d'authentification inhabituelle, conservee a l'identique.** L'application
   d'origine n'authentifie PAS contre un mot de passe applicatif stocke en base. Elle ouvre une
   connexion JDBC brute vers Oracle en utilisant le login/mot de passe saisis comme identifiants
   de connexion a la base elle-meme (`Userconnexion.connecterUser()`), puis va chercher le
   profil dans `MBUDGET.UTILISATEUR_LOG`. Ce portage reproduit fidelement ce mecanisme dans
   `security/LegacyDbAuthenticationProvider.java`. **Chaque utilisateur de l'appli doit donc
   avoir un compte Oracle valide.** C'est une architecture couplee fortement a la base et
   inhabituelle pour une appli web : a moyen terme, il est recommande de migrer vers un
   stockage applicatif (table utilisateurs + mot de passe hache BCrypt + Spring Security
   classique) plutot que des comptes Oracle par utilisateur.

3. **Doublon `Users` / `UsersEposte`.** Les deux entites JPA mappent la meme table
   `USERS_EPOSTE` (c'etait deja le cas dans le code legacy — deux DAO differents pour la meme
   table). `Users` est utilise pour tout ce qui touche profil/mot de passe/email/activation
   (confirme en lisant `UserCtrl2.java`). A terme, il faudrait fusionner ces deux entites en
   une seule pour eviter toute divergence de cache JPA (deux entites JPA différentes pointant
   sur la même ligne physique peuvent désynchroniser le premier-level cache d'Hibernate).

4. **Compilation non verifiee dans cet environnement.** Le sandbox utilise pour ce portage
   n'a pas d'acces reseau vers Maven Central (`repo.maven.apache.org` renvoie 403 via le proxy
   sortant). Le code a ete relu manuellement (imports, signatures, accolades equilibrees) mais
   **`mvn clean compile` n'a pas pu etre execute avec succes ici**. C'est la toute premiere
   chose a faire des reception de ce projet, avant toute autre verification.

5. **Plusieurs ecrans "Etats" reconstruisent des requetes SQL approximees.** Beaucoup de
   rapports legacy (menu "Etats", "Marchand", "Commissions", "Recap") ne generaient a l'origine
   qu'un export Jasper compile (`.jasper` binaire) dont le SQL source n'existe plus dans le
   depot (seuls quelques `.jrxml` ont survecu, copies dans `src/main/resources/reports/`).
   Pour ces ecrans, la requete a ete reconstruite par deduction (schema `SPT.*`/`EPOSTE.*`
   observe ailleurs dans le code). Chaque cas est documente par un commentaire Javadoc ou un
   `<!-- TODO -->` dans le controller/template concerne — **cherchez `TODO` dans le code avant
   toute mise en prod** pour la liste complete. Deux ecrans (`/eposte/chequiers`,
   `/eposte/achats-credit`) pointent vers des tables (`CHEQUIER`, `ACHAT_CREDIT`) qui n'apparaissent
   nulle part dans le code source legacy — schema plausible mais **non confirme**.

6. **Changement de mot de passe = portee limitee.** `/parametres/modif-password` ne modifie
   que la colonne `CODE_SECRET`/`CODE_PASSE` de la table `USERS_EPOSTE` (comme le faisait
   l'ecran d'origine) — cela ne change PAS le mot de passe reel du compte Oracle utilise pour
   se connecter a ce back-office (voir point 2). C'est une limitation heritee du legacy, pas
   un bug de ce portage.

## Ce qui a ete porte

Sur les 80 pages JSF (`web/*.xhtml`) de l'application d'origine :

- **~75 ecrans fonctionnels** ont ete portes en controllers Spring MVC + templates
  Thymeleaf reels (formulaire de filtre -> requete -> tableau de resultats), pas des stubs.
- Quelques ecrans quasi-identiques ont ete regroupes sur une seule route quand le code source
  le justifiait (ex. `bb`/`bbmoov`, `bw`/`bwflooz` regroupes par variante d'operateur plutot
  que dupliques).
- Non portes (backlog mineur, voir plus bas) : `About.xhtml`, `Error404.xhtml`,
  `chargerFichier.xhtml` (widget d'upload generique), `listereleve.xhtml` (page qui ne semble
  plus etre liee depuis le menu — probablement du code mort a confirmer).

### Modules

| Domaine | Controllers | Statut |
|---|---|---|
| Authentification | `LoginController`, `security/*` | Fait (fidele a l'original, voir point 2 ci-dessus) |
| Accueil / Menu | `HomeController`, `fragments/menu.html` | Fait |
| Comptes | `ComptesController`, `CompteLieController` | Fait |
| Operations / Transactions | `OperationConsultationController`, `TransactionEtatController`, `AnnulationOpController`, `GatewayController`, `EposteOperationController` | Fait (quelques approximations, voir TODO) |
| Reclamations / Suggestions / Support | `ReclamationController`, `TypeReclamationController`, `SuggestionController`, `SupportController` | Fait |
| Releves + JasperReports | `ReleveController`, `ReleveExportController`, `JasperReportService` | Fait |
| Marchand / Commissions / Recap | `MarchandController`, `CommissionsController`, `RecapController`, `EposteReportController`, `SuggestionsReportController` | Fait (approximations sur certains ecrans, voir TODO) |
| Profil / Mot de passe / Email / Activation | `ProfilController`, `PasswordResetController`, `EmailUpdateController`, `CompteActivationController`, `MarchandParametrageController` | Fait |

### Backlog restant

| Ecran legacy | Pourquoi non porte | Effort estime |
|---|---|---|
| `About.xhtml` | Page statique d'information, faible priorite | Trivial (~15 min) |
| `Error404.xhtml` | Page d'erreur — Spring Boot a sa propre gestion d'erreur par defaut a personnaliser | Petit (~30 min) |
| `chargerFichier.xhtml` | Widget d'upload generique — verifier d'abord ou/si il est reellement utilise ailleurs dans l'appli avant de le porter isolement | A investiguer |
| `listereleve.xhtml` | Ne semble plus etre lie depuis `menu.xhtml` — probablement du code mort, a confirmer avec l'equipe metier avant de decider de le porter | A investiguer |

## Lancer le projet

```bash
export DB_URL="jdbc:oracle:thin:@//<host>:1521/<service>"
export DB_USERNAME="..."
export DB_PASSWORD="..."
mvn clean spring-boot:run
```

Toutes les variables d'environnement disponibles sont documentees dans
`src/main/resources/application.yml`.

## Prochaines etapes recommandees

1. `mvn clean compile` dans un environnement avec acces a Maven Central, corriger les
   eventuelles erreurs de compilation residuelles.
2. Faire tourner les identifiants Oracle exposes dans le repo d'origine (point 1 ci-dessus).
3. Revue de securite de `LegacyDbAuthenticationProvider` (point 2) — decider si l'architecture
   d'authentification par compte Oracle est acceptable a long terme ou doit etre remplacee.
4. Chercher tous les commentaires `TODO` dans le code (`grep -rn "TODO" src/main`) et les
   traiter un par un avec la connaissance metier de l'equipe.
5. Tests d'integration contre une vraie base Oracle de recette pour valider les requetes
   reconstruites (point 5).
6. Porter le backlog restant (tableau ci-dessus).
