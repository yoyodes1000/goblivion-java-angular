# Ticket 2 — Architecture

Note de relecture. Objectif : que tu comprennes **pourquoi** chaque choix a été
fait, pas seulement ce qui a été généré.

## Ce qui existe maintenant

```
backend/     Spring Boot 4.1.0, Maven, cible Java 21
frontend/    Angular 21, standalone, zoneless, SCSS, tests Vitest
docs/        regles/ (à venir) et notes/ (ce fichier)
.gitignore   à la racine, couvre les deux sous-projets + les scans
```

Un seul dépôt git, à la racine. Les deux sous-projets vivent dedans mais gardent
chacun leur outillage : `mvnw` côté backend, `npm` côté frontend.

Vérifié : `./mvnw test` passe (1 test), `npm test` passe (2 tests).

## Les choix, et pourquoi

### Un seul dépôt (monorepo) plutôt que deux

Tu n'as qu'un dépôt GitHub, et le backend et le frontend vont bouger ensemble à
chaque évolution du modèle de cartes. Deux dépôts imposeraient de synchroniser
deux commits pour un seul changement fonctionnel — pénible à deux, absurde seul.

Contrepartie : tu ne peux pas déployer les deux séparément sans un peu de
configuration. Sans objet ici, le jeu tourne sur ta machine.

### Java 21 alors que ton JDK est un 23

`<java.version>21</java.version>` dit à Maven de produire du bytecode Java 21 et
de **refuser** toute API introduite après 21. Le JDK 23 installé compile très
bien vers cette cible.

Pourquoi ne pas viser 23 ? Parce que 23 n'est pas une version LTS et n'est déjà
plus supportée. 21 est la version LTS que tu retrouveras partout en entreprise,
et elle a tout ce dont on a besoin : `record`, `sealed`, pattern matching sur
`switch`. On s'en servira beaucoup pour modéliser les cartes.

### Spring Boot 4.1 — attention aux noms de starters

Boot 4 a découpé les starters de test. Tu ne verras pas dans notre `pom.xml` ce
que montrent tous les tutoriels :

| Tutoriels (Boot 3) | Notre `pom.xml` (Boot 4) |
|---|---|
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| `spring-boot-starter-test` | `spring-boot-starter-webmvc-test`, `spring-boot-starter-validation-test` |

Si tu copies un bout de `pom.xml` trouvé en ligne et que Maven ne résout rien,
c'est presque toujours ça.

Au passage : `start.spring.io` annonce la version `4.1.0.RELEASE` dans son API,
mais l'artefact publié s'appelle `4.1.0` tout court. Le suffixe `.RELEASE` a
disparu après Boot 2.3. Première version générée : build cassé, corrigé dans le
`pom.xml`.

### Angular « zoneless »

Le projet est généré **sans `zone.js`** — regarde `package.json`, la dépendance
n'y est pas. Historiquement Angular patchait toutes les API asynchrones du
navigateur pour deviner quand rafraîchir l'affichage. Depuis la v21, c'est fini
par défaut : on déclare l'état avec des **signaux**, et Angular ne rafraîchit que
ce qui dépend d'un signal modifié.

Concrètement, dans `app.ts` :

```ts
protected readonly titre = signal('Goblivion');
```

et dans le template `{{ titre() }}` — avec les parenthèses, on *lit* le signal.
C'est ce qui crée le lien entre la donnée et l'affichage.

`changeDetection: ChangeDetectionStrategy.OnPush` complète le dispositif. À
mettre sur tous nos composants ; ça deviendra le défaut d'Angular.

Ce que ça implique pour toi : **la quasi-totalité des tutoriels Angular en ligne
sont écrits pour l'ancien modèle** (`NgModule`, `*ngIf`, `zone.js`, RxJS
partout). Ils fonctionnent encore, mais ce n'est plus ce qu'on écrit.

### Vitest plutôt que Karma

Karma était le lanceur de tests historique ; il ouvre un vrai navigateur. Vitest
tourne dans Node avec `jsdom`, démarre en une seconde, et c'est le défaut de la
CLI v21. Pour un moteur de jeu qu'on va tester en boucle, la vitesse compte.

### Pas de base de données

Tes données de cartes sont déjà des fichiers JSON, et une partie de Goblivion
tient entièrement en mémoire. Ajouter PostgreSQL ou JPA ici, ce serait beaucoup
de configuration pour zéro gain, et surtout du temps d'apprentissage volé au
moteur de jeu — qui est la partie intéressante.

Si tu veux quand même faire du JPA pour l'exercice, dis-le : on l'ajoutera sur
une fonctionnalité qui le justifie (un historique de parties, par exemple).

## Ce que j'ai écarté

| Écarté | Pourquoi |
|---|---|
| Gradle | Maven est plus verbeux mais plus lisible quand on débute, et c'est ce que tu croiseras le plus |
| `NgModule` | remplacé par les composants standalone depuis la v16 |
| SSR / prerendering | inutile pour une application locale, ça ne ferait qu'ajouter un serveur Node |
| JPA + PostgreSQL | voir ci-dessus |
| Lombok | masque le code généré ; les `record` Java 21 couvrent 90 % du besoin |

## À regarder dans le code

1. `backend/pom.xml` — le bloc `<parent>` : d'où viennent les versions des
   dépendances qu'on n'a jamais déclarées.
2. `backend/src/main/java/fr/goblivion/GoblivionBackendApplication.java` — une
   seule annotation, `@SpringBootApplication`, et tout démarre. On regardera ce
   qu'elle cache quand on écrira le premier contrôleur.
3. `frontend/src/main.ts` et `app.config.ts` — le démarrage d'une application
   Angular moderne : `bootstrapApplication` + une liste de `providers`.
4. `frontend/src/app/app.ts` — signal, `OnPush`, template externe.

## Trois questions pour vérifier

1. Dans `app.html`, pourquoi `{{ titre() }}` et pas `{{ titre }}` ?
2. Si tu ajoutes `spring-boot-starter-data-jpa` au `pom.xml` sans numéro de
   version, où Maven va-t-il le chercher ?
3. Notre `pom.xml` cible Java 21. Que se passe-t-il si j'écris du code qui
   utilise une API arrivée en Java 22 ?

## Reste à faire sur ce ticket

- [ ] Premier commit et poussée vers GitHub (en attente de ton feu vert)
- [ ] Décider du sort des données de cartes : versionnées ou locales
