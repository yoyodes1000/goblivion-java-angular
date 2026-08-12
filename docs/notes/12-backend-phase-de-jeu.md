# Ticket 12 — Backend : phases de jeu

Note de relecture. Le ticket demandait « la mise en place des différentes phases
de jeu et le découpage des actions possibles dans les différentes phases ».
C'est le **premier code moteur** du projet : avant lui, `backend/` ne contenait
qu'un `@SpringBootApplication` vide, et le frontend simulait tout.

## Ce qui existe maintenant

```
backend/src/main/java/fr/goblivion/
├── cartes/        les 74 cartes en records, et leur chargement
│   ├── Paysan            interface scellee : Bleue, Doree, moitie objet
│   ├── Catalogue         les cinq listes, resolvables par id
│   └── ChargeurCartes    lit data/cartes/, tolere son absence
├── partie/        l'etat, les regles, le moteur
│   ├── Phase             les quatre etats d'une partie
│   ├── Difficulte        3, 4 ou 5 boss — et ce que ca change d'autre
│   ├── TypeAction        le decoupage : chaque action porte ses phases
│   ├── CarteEnJeu        un exemplaire, pas un type
│   ├── Partie            les zones et les operations elementaires
│   ├── MiseEnPlace       le §3 : les tirages
│   ├── MoteurPartie      ce que le joueur peut faire, et ce qui s'ensuit
│   └── ServicePartie     la partie en cours — il n'y en a qu'une
└── api/           trois routes, pas d'identifiant de partie
```

Côté frontend, `src/app/partie/` est neuf : le modèle miroir de l'API, le
service, l'écran de difficulté et le panneau d'actions. Le plateau, lui, a
perdu tout son état de jeu.

Vérifié : 70 tests backend, 64 frontend, et une partie jouée de bout en bout
dans le navigateur contre le vrai jeu de données.

## Le choix qui structure tout : le découpage est le code

`TypeAction` porte lui-même la liste des phases où chaque action a sa place :

```java
CHOISIR_ENTRAINEMENT(Phase.ENTRAINEMENT),
ECHANGER_GARDE_DU_CORPS(Phase.ENTRAINEMENT, Phase.COMBAT, Phase.BOSS),
PHASE_SUIVANTE(Phase.values());
```

Le tableau du ticket n'est donc pas une documentation à maintenir en parallèle
du code — il **est** le code. `TypeAction.permisesEn(phase)` s'envoie dans
l'état, et le frontend s'en sert pour n'afficher que des boutons qui marchent.
C'est la seule façon d'être sûr que les deux ne divergeront pas.

Le refus est un résultat de premier rang, pas un cas d'erreur : jouer, c'est
d'abord savoir ce qu'on ne peut pas faire. Toute `ActionInterdite` porte un
motif rédigé, qui remonte tel quel jusqu'à l'écran.

## Trois résultats de lecture des règles

Ce ne sont pas des choix d'implémentation, ce sont des choses que la lecture
attentive du livret impose — et qu'on n'aurait pas devinées.

### La phase d'Avancée n'accepte aucune action

Elle n'a qu'une entrée dans le tableau : `PHASE_SUIVANTE`. « L'Ennemi Avance »
ne comporte **aucune décision du joueur** (§7) : tout s'y passe à l'entrée de
la phase, le joueur constate puis passe au combat. Une phase sans action est un
résultat, pas un trou. `TypeActionTest` l'écrit noir sur blanc, pour que
quiconque y ajoutera un bouton un jour soit obligé de relire le §7 d'abord.

### Piocher n'est pas une action du joueur

On ne pioche jamais parce qu'on le décide : on pioche parce qu'on a posé le
jeton d'entraînement (§6.2), parce qu'un ennemi l'exige (§8.1) ou parce qu'une
carte le demande (§11). C'est une **conséquence**, donc une opération interne du
moteur — elle n'a pas sa place dans le catalogue des actions.

### Le moteur doit retenir *quand* une carte a été révélée

L'action d'un ennemi ne part **qu'au tour de sa révélation** (§7). Retenir
seulement *si* la carte est révélée suffirait à perdre la règle : un ennemi
retourné plus tôt par une Vision arrive aux Portes déjà révélé, et son action ne
partira jamais. D'où `tourRevelation` à côté de `revelee`, et
`actionDeclenchableAu(tour)`.

C'est ce qui fait de la Vision le principal outil pour neutraliser les actions
ennemies — une carte, pas un hasard.

## Les décisions, et pourquoi

### Un dossier `data/cartes/` absent ne fait pas échouer le démarrage

Les données de cartes sont du contenu Goblivion Games, exclu d'un dépôt public.
L'agent d'intégration continue clone un dépôt qui n'en a pas : un chargeur qui
échouerait rendrait la CI rouge en permanence, donc inutile. Il prévient dans
les journaux et rend un catalogue vide ; le refus arrive plus tard, à la
création d'une partie, là où il peut être **expliqué** — un 503 avec son motif.

Les tests, pour la même raison, travaillent sur un jeu de cartes **inventé**
(`CataloguesFictifs`). Les quantités, elles, reproduisent le vrai matériel — 40
Bleues, 12 ennemis à 1 épée et 11 à 2 épées — parce que la mise en place en
dépend : tirer 20 sur 40 n'a de sens que si les comptes tiennent.

### Les tirages portent sur les exemplaires, pas sur les types

Il y a 25 types de Bleues mais **40 cartes**, dont douze Fermiers. Tirer 20
types n'aurait aucun sens, et interdirait de commencer avec deux Fermiers — ce
qui est pourtant le cas le plus probable. D'où `CarteEnJeu`, qui a une identité
propre indépendante de son type : « détruire le Fermier » ne veut rien dire,
« détruire l'exemplaire 17 » si.

### Le rôle est tiré au sort

Le livret fait **choisir** un rôle (§3) ; tu as demandé un tirage. C'est le
défaut, et imposer un rôle reste possible — les tests en vivent, et le choix
redeviendra offert le jour où l'interface le proposera.

### Difficile ne rajoute pas une avancée par tour

Le §3 est précis : la partie **commence** par la phase « L'Ennemi Avance ». Un
ennemi met toujours quatre avancées à atteindre les Portes ; il y arrive donc au
3e tour au lieu du 4e. C'est un tour d'entraînement en moins, pas un rythme
d'avancée doublé.

### La répartition est validée avant d'être appliquée

Combat perdu, le joueur peut quand même abattre les ennemis dont il égale
exactement la force (§8). Le moteur vérifie que la répartition tient **avant**
de retirer la moindre ressource. L'ordre n'est pas cosmétique : refuser après
avoir prélevé laisserait la partie à moitié modifiée par une action qui a
échoué. Un test le verrouille.

### Chaque action rend l'état complet

Une action de Goblivion touche facilement quatre zones à la fois — piocher vide
le Château, remplit le Champ de bataille, peut mélanger l'Hôpital et faire
avancer l'ennemi. Renvoyer un compte-rendu du changement obligerait le frontend
à rejouer ces conséquences pour rester synchronisé, c'est-à-dire à
réimplémenter les règles. Il reçoit l'état entier.

### Les cartes ne voyagent que par leur identifiant

Le frontend charge déjà le catalogue pour composer ses adresses de scans. Les
renvoyer sur chaque requête ferait de chaque état une copie du contenu Goblivion
Games. **L'API dit où sont les cartes ; le catalogue dit ce qu'elles sont.**

Deux exceptions, parce que le frontend ne pourrait pas les recalculer sans
réimplémenter les règles : les forces en présence, et `actionsPossibles`.

### La phase et la famille voyagent dans le vocabulaire du frontend

`@JsonValue` fait sérialiser `Phase.ENTRAINEMENT` en `"entrainement"` et
`Famille.ENNEMIS_OBJETS` en `"ennemis-objets"`. C'est déjà ce que le frontend
écrit — ses libellés, ses dossiers de scans, et les sélecteurs `[data-phase='…']`
de `styles.scss`. Faire porter la conversion par l'API évite une table de
correspondance côté navigateur, et surtout évite qu'un jour les deux listes
divergent.

### Un mandataire plutôt que CORS

Le serveur de développement Angular renvoie `/api` vers Spring
(`proxy.conf.json`). Vu du navigateur il n'y a qu'une seule origine, donc aucun
en-tête CORS à ouvrir, à maintenir, ni à refermer le jour où le backend servira
le frontend.

## Ce que Spring Boot 4 change, et qui surprend

**Jackson 3.** Boot 4 est passé de `com.fasterxml.jackson` à `tools.jackson` —
nouveau groupe, nouveau paquetage. Aucun tutoriel en ligne ne le dit encore.
Deux conséquences visibles :

- l'`ObjectMapper` est **immuable** : il se construit par `JsonMapper.builder()`,
  pas par `new` suivi de `disable()` ;
- les erreurs de lecture sont des `JacksonException` **non contrôlées** — le
  compilateur ne rappellera pas de les attraper.

Les annotations, elles, sont restées en `com.fasterxml.jackson.annotation` :
`@JsonValue` s'importe encore de là. Le mélange est déroutant et c'est normal.

**`@WebMvcTest`** a changé de paquetage :
`org.springframework.boot.webmvc.test.autoconfigure`, plus
`…boot.test.autoconfigure.web.servlet`.

## Un piège de test qui vaut d'être noté

`ServicePartie` tient **la** partie en cours, et Spring garde ses contextes en
cache d'un test à l'autre. Sans `@DirtiesContext`, la partie créée par un test
survivait au suivant, et « aucune partie en cours » devenait intestable : le
résultat dépendait de l'ordre d'exécution. Le symptôme est un test qui passe
seul et échoue en suite.

## Ce que j'ai écarté

| Écarté | Pourquoi |
|---|---|
| Transcrire les effets des cartes | c'est le ticket 11 ; ici on décide *quand* un effet a le droit de partir, pas *ce qu'il fait* |
| Un identifiant de partie dans l'URL | le jeu est solo et local, il n'y en a qu'une |
| Une base de données | une partie tient en mémoire ; la persistance, si elle vient, sera une sauvegarde JSON |
| Réimplémenter des règles côté navigateur pour griser un bouton plus vite | deux moteurs finissent par ne plus être d'accord |
| Faire échouer le démarrage sans données de cartes | la CI n'en a pas, et elle doit rester verte |
| Un plancher à zéro sur les ressources | ce serait masquer la défaite ; c'est au moteur de la prononcer |
| Rendre les ressources versées à un entraînement abandonné | on paie à l'étape 4 pour accéder à l'étape 5 ; abandonner en 5 est un choix (§6) |
| Afficher les ennemis sur la piste et aux Portes | c'est le ticket 10 ; l'état les transporte déjà, il ne reste qu'à les dessiner |

## Reste à faire

- [ ] **Le Joker n'apporte rien.** Il copie un Paysan Humain en jeu : la cible
      est un choix du joueur, donc une action — ticket 11. En attendant il vaut
      0, et le code le dit plutôt que d'inventer une valeur.
- [ ] **Les effets des cartes ne partent pas.** Pivoter marque la carte comme
      activée, le pouvoir royal se marque comme dépensé, l'action d'un ennemi
      est notée au journal — mais rien ne s'exécute. Ticket 11.
- [ ] **Les jetons Bonus Allié se comptent sans se dépenser.** Facile en offre
      trois ; ce qui les pose sur une carte relève des actions.
- [ ] **La piste et les Portes ne montrent rien.** L'état porte `piste` et
      `portes`, avec la carte masquée tant qu'elle est face cachée. Ticket 10.
- [ ] **Pas de rejouer une partie sans recharger la page.** L'écran de fin
      annonce le résultat mais n'offre pas de repartir.
