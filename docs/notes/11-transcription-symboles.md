# Ticket 11 — Backend : transcription des symboles en effets

Note de relecture. Le ticket demandait « de retranscrire sous forme de code les
effets des cartes », avec une checklist d'un seul point : *à valider au cas par
cas*. Le ticket 12 avait décidé **quand** un effet a le droit de partir ; celui-ci
décide **ce qu'il fait**.

## Ce qui existe maintenant

```
backend/src/main/java/fr/goblivion/
├── effets/          le vocabulaire, sans aucune dependance au moteur
│   ├── Effet             28 briques, interface scellee
│   ├── Declencheur       9 moments ou un effet peut partir
│   ├── Cible             13 cibles, dont celles qui demandent un choix
│   ├── Duree             immediate, phase, combat, permanente
│   ├── Quantite          ce qui se compte dans un « pour chaque »
│   └── EffetCarte        un effet et son declencheur
│   └── PlanDeCiblage     ce qu'un effet reclamera, connu avant de le jouer
└── partie/
    └── InterpreteEffets  l'execution, en deux passes
```

Côté frontend, `partie/ciblage/` pose au joueur les questions que le plan
annonce, et la zone de jeu n'offre plus « Pivoter » qu'aux cartes qui ont
quelque chose à déclencher.

Les données de cartes portent un champ `effets` à côté de `action`. **73 effets
transcrits** couvrant les 75 actions imprimées — le Soldat et son jumeau Objet
portent un texte dont la règle vit dans `forceVariable`, donc une transcription
vide et assumée.

Vérifié : 142 tests backend et 72 frontend. Deux des tests backend lisent les
vraies cartes en local et s'abstiennent en intégration continue. Le ciblage a
aussi été joué dans le navigateur, contre le vrai jeu de données.

## Le choix qui structure tout : la transcription est une donnée

Trois formes étaient possibles. Analyser le texte français de `action`, tenir un
registre en Java indexé par identifiant de carte, ou ajouter un champ structuré
aux données. C'est le troisième qui a été retenu, et pour une raison qui n'est
pas technique : **le dépôt est public, les cartes sont du contenu Goblivion
Games.** Un registre en code y ferait entrer 74 identifiants et leurs effets par
une autre porte. Le parseur, lui, rendrait le moteur dépendant du libellé exact —
une virgule déplacée dans les données casserait une règle, et les tests, qui
travaillent sur des cartes inventées, devraient imiter la prose pour tester quoi
que ce soit.

`action` et `effets` **coexistent et ne se déduisent pas l'un de l'autre**.
L'un est ce que le joueur lit, mot pour mot ; l'autre ce que le moteur exécute.

Corollaire assumé : la table de correspondance qui a servi à la transcription
vit hors dépôt, elle aussi. Elle a tourné une fois, et les données la portent
désormais.

### Le vocabulaire est fermé, et le compilateur le garde

`Effet` est une interface **scellée**. Une carte qui ne s'exprime pas avec les
briques existantes n'est pas un cas à contourner : c'est une brique qui manque.
Le `switch` de l'interprète étant exhaustif, ajouter une brique fait échouer la
compilation à l'endroit exact qui doit savoir quoi en faire.

Le Roi Yolo l'a prouvé pendant la transcription : « Mélange le Château » n'est
pas « Mélange l'Hôpital à ton Château ». Rien n'entre, on perd seulement l'ordre
connu — ce qui est précisément le prix à payer pour avoir fouillé le paquet à la
ligne précédente. Vingt-huitième brique.

## Cinq lectures qui ont changé le code

Ce ne sont pas des choix d'implémentation. Ce sont des réponses de relecture qui
ont modifié le vocabulaire lui-même.

### Une durée manquait

Le Joker prend **toutes** les caractéristiques de sa cible — force et action
comprises — jusqu'à la fin de la phase, puis redevient un Joker. Le Héros du
village fait la même chose sous un autre nom. J'avais traité la copie comme un
calcul de force ; c'est une substitution d'identité temporaire, ce qui est un
tout autre travail.

Bénéfice inattendu : la durée sépare deux effets que j'écrivais pareil. Le
Goblinosaurus et le Gobelin Pestilent ignorent tous deux les jetons Bannière,
mais l'un tant qu'il est là, l'autre pour un seul combat. Sans `Duree`, la
transcription les aurait confondus en silence.

### Le Héros du village *devient* un Soldat

Il ne prend pas la force d'un Soldat, il entre dans le total dont dépend la
force de **tous** les autres (§12). Un Soldat seul vaut 2 ; le Héros arrive, ils
valent 3 chacun. En prendre seulement la valeur aurait laissé le vrai Soldat à 2.

### La carte royale se retourne, elle ne pivote pas

Cinq cartes sur sept impriment `Pivoter:`, mais le geste n'en est pas un. Les
sept se déclenchent donc pareil, y compris le Roi Brad et la Reine Jade dont le
texte ne porte aucun préfixe. Ça explique aussi le Hochet royal : « réactive une
carte Roi/Reine », c'est la remettre à l'endroit, donc rendre le pouvoir.

### L'action d'un Boss repart à chaque assaut

Celle-là, le ticket 12 l'avait **déjà tranchée** — « l'action du Boss repart à
chaque tentative ». Les cinq Boss à effet ponctuel étaient partis sur
`REVELATION`, ce qui les aurait fait frapper une seule fois : un Boss raté au
premier essai serait devenu inoffensif aux suivants. D'où `ASSAUT_BOSS`, distinct
de `REVELATION` qui ne part qu'une fois et de `PERMANENT` qui ne part jamais.

Leçon de méthode : la réponse était dans le code, pas dans le livret. Relire ce
que le ticket précédent a décidé vaut mieux que redécider.

### Le Testament part à la destruction, pas à la défausse

Une carte défaussée rejoint l'Hôpital et reviendra : elle n'a rien légué. Sans ce
partage, l'Alchimiste — qui défausse — ferait du Duc une rente à 3 ressources par
tour. Deux tests tiennent la même situation à un mot près.

### La Vision ne regarde pas le Château

La plus grosse erreur du ticket, et elle était dans ma transcription, pas dans le
code. J'avais documenté `Visionner` comme « regarder puis réordonner le dessus du
Château ». Le livret dit autre chose : « certaines actions alliées **révèlent une
carte Ennemi** aux emplacements marqués du plateau ».

Transcrite comme je l'avais écrite, la brique aurait inventé un effet qui
n'existe pas — et personne ne s'en serait aperçu avant de jouer, puisque les deux
lectures sont plausibles pour qui ne relit pas le §7.

Elle retourne donc **le plus avancé des ennemis cachés** : celui dont l'action va
partir le plus tôt, donc le seul qu'il soit encore utile de neutraliser. Retourner
une carte du fond du paquet ne coûterait à personne.

C'est aussi ce qui donne son sens à la règle du ticket 12 : révélé avant le tour
où il arrive, l'ennemi ne lancera pas son action. La Vision est un effet
**défensif** déguisé en effet de pioche.

## Les décisions, et pourquoi

### Les choix du joueur arrivent avec l'action, pas après

Une cible au singulier consomme une désignation dans une file jointe à la
demande ; une file trop courte est un refus rédigé. Pas d'état « en attente de
choix » dans le moteur, donc jamais de partie à moitié modifiée en train
d'attendre une réponse.

**19 des 73 effets** exigent au moins une désignation. C'est aussi ce qui
sépare, dans `Cible`, ce qui commence par `UN_`/`UNE_` de ce qui commence par
`CHAQUE_` — la première famille suspend, la seconde se résout seule.

### Vérifier d'abord, appliquer ensuite

Un effet touche facilement plusieurs zones, et une désignation manquante
n'apparaît qu'au milieu du parcours : « Détruis une carte en jeu puis Piocher 1 »
aurait déjà détruit quand il refuse. L'interprète parcourt donc l'effet **deux
fois** — une passe de vérification sur une copie des réponses, sans rien
modifier, puis l'application.

Un seul parcours sert aux deux, avec un drapeau. Deux méthodes séparées auraient
fini par diverger, et la vérification aurait approuvé ce que l'exécution refuse.

**C'est un test qui a imposé ça.** Le premier branchement marquait la carte
activée *avant* de lancer son effet : une désignation oubliée, et le joueur
perdait le Pivoter de sa carte sans rien recevoir. Le pouvoir royal avait le même
défaut. C'est le principe que le ticket 12 applique déjà à la répartition du
combat — valider avant de prélever.

Un exemplaire ne peut pas non plus être désigné deux fois dans un même effet :
sans ce suivi, défausser deux fois la même carte passait la vérification, qui ne
retire rien, puis échouait à l'application.

### Détruire n'est pas défausser

Défausser envoie à l'Hôpital, d'où la carte revient un jour ; détruire la sort de
la partie. Les confondre changerait l'économie du deck — l'Alchimiste défausse,
le Bourreau détruit. Un test le verrouille dans les trois zones à la fois.

Dans la même veine : mélanger l'Hôpital au Château **redresse** ce qui revient.
Sans ça, chaque passage par l'Hôpital laisserait une carte définitivement inerte
et le deck s'éteindrait à petit feu.

### Un effet déclenché par le moteur ne refuse pas, il se note

Une révélation d'ennemi ou un Testament arrivent au milieu d'un tour : le joueur
n'a rien pu joindre à une carte qu'il n'avait pas vue venir. Un refus n'aurait
personne à qui s'adresser et bloquerait le tour.

Donc : ce qui peut partir part, le reste est inscrit au journal en toutes lettres
— « *Sorcière Troll : effet non appliqué — cette action demande de désigner un
paysan Humain* ». L'écart avec le jeu de plateau reste **visible** plutôt que
silencieux. Et grâce à la double passe, ce qui est noté est un effet *non
appliqué*, jamais à moitié appliqué.

Concrètement : 10 des 13 révélations fonctionnent en jeu, trois s'inscrivent au
journal en attendant.

### L'interface lit le plan, elle ne le devine pas

Le moteur annonce, pour chaque carte en jeu, ce que son action réclamera : les
désignations **dans l'ordre où l'interprète les consommera**, et les branches
d'un `ou`. L'écran pose ces questions ; il ne les déduit pas.

Les deux autres chemins étaient pires. Rejouer le vocabulaire des effets côté
navigateur en aurait tenu une seconde version, qui aurait fini par ne plus être
d'accord avec la première — c'est exactement ce qu'on a refusé pour le tableau
des phases au ticket 12. Envoyer l'action et afficher le refus aurait fait
deviner le joueur.

L'ordre commande la présentation : **une question à la fois, jamais une liste à
cocher**. Deux identifiants d'exemplaires se ressemblent, et une réponse rendue
dans le désordre ferait détruire la mauvaise carte sans que rien ne s'en
aperçoive.

Deux canaux séparés, parce que deux choses différentes se désignent : un
**exemplaire** posé sur la table a une identité numérique ; un **type** de carte
au Marché n'est pas encore en jeu et n'en a pas. `Designation.parType` dit lequel
des deux l'écran doit proposer.

### Un plan vide ne veut pas dire « pas d'action »

Le Boulanger agit sans rien réclamer, un Fermier n'agit pas du tout, et les deux
ont un plan vide. D'où un drapeau distinct, `agitAuPivot` : sans lui, l'interface
proposait « Pivoter » sur des cartes qui n'avaient rien à déclencher — un bouton
que le moteur accepte en ne faisant rien, ce qui est pire qu'un refus puisque
rien ne le dit.

### Les passifs de Boss ne s'exécutent pas, ils se consultent

Les cinq effets continus ne changent pas l'état, ils changent la **lecture**
qu'on en fait. Et ils ne sont pas d'une seule nature : trois points de
branchement, pas un.

| Passif | Où |
|---|---|
| Ignorer les jetons Bannière | `forceEffective()` |
| Ignorer la force des Objets | `forceEffective()` — la force tombe, les jetons restent |
| Ignorer la force à partir d'un seuil | `forceEffective()` |
| Réduire les doublons | `forceAlliee()` — un total, pas une carte |
| Priver de ressources | `gagnerRessources()` — ni force ni état |

**Le seuil de la Trollette porte sur la force imprimée.** Sinon poser des jetons
sur une carte de force 1 la ferait franchir le seuil de 4 et disparaître :
renforcer une carte l'aurait détruite. Un test tient ce cas seul.

Les passifs n'ont **rien à défaire** en fin de phase : ils se déduisent de la
position des cartes. Le Boss est là ou il ne l'est pas. C'est ce qui rend
`Duree.COMBAT` et `Duree.PERMANENTE` descriptives plutôt qu'actives — seule
`Duree.PHASE` demande un nettoyage, et la couture existait déjà.

Un ennemi encore face cachée n'impose rien : on ne subit pas ce qu'on n'a pas
retourné. La Vision garde son rôle, comme au ticket 12.

## Ce que Jackson réserve, deuxième épisode

**Le discriminant occupe le nom `type`.** `ObtenirDuMarche` portait un composant
`type` — deux `type` dans le même objet JSON, et l'un des deux disparaît sans
bruit. Renommé `typeCarte`. Aucun autre champ de brique ne peut s'appeler `type`.

**Les annotations de polymorphisme sont restées en
`com.fasterxml.jackson.annotation`** là où le reste de Jackson est passé en
`tools.jackson`. Le mélange fonctionne, et un test le verrouille.

**Une brique non déclarée ne lève rien.** Elle compile, se sérialise sans son
`type`, et ne se relit jamais — visible seulement au chargement des vraies
données, donc jamais en CI. D'où un test qui compare les sous-classes permises de
l'interface scellée à ce que `@JsonSubTypes` déclare.

## Deux tests qui valent d'être notés

`EffetsDesVraiesDonneesTest` lit `data/cartes/` **s'il est là** et s'abstient
sinon. En intégration continue il ne s'exécute pas ; en local il est le seul
capable de dire qu'un nom d'effet est mal orthographié. Il ne vérifie pas
l'absence d'exception mais les **comptes** : une transcription illisible rendrait
une liste vide, et le moteur exécuterait le silence.

`CataloguesFictifs` a gagné trois fabriques — un effet sur un Humain, un passif
de Boss, un passif d'ennemi — qui **conservent tous les comptes**. Mise en place
et tirages se comportent exactement pareil ; seule la carte agit.

## Ce que j'ai écarté

| Écarté | Pourquoi |
|---|---|
| Analyser le texte français de `action` | le moteur dépendrait du libellé exact, et les tests devraient imiter la prose |
| Un registre `id → effet` en Java | 74 identifiants de contenu Goblivion dans un dépôt public |
| Garder la table de transcription dans le dépôt | même raison, par une autre porte |
| Un état « en attente de désignation » | un moteur, une API et une interface à suspendre, pour cinq cartes |
| Déduire côté navigateur ce qu'un effet réclame | une seconde version du vocabulaire, qui finirait par diverger |
| Envoyer l'action et afficher le refus | faire deviner le joueur ce qu'il aurait dû désigner |
| Filtrer les candidats par type à l'écran | il faudrait y connaître les conditions de chaque cible ; le moteur refuse, et sa double passe garantit que rien n'a bougé |
| Choisir à la place du joueur sur un effet automatique | le moteur déciderait quel paysan la Sorcière Troll emporte |
| Exécuter à moitié puis refuser | la partie resterait modifiée par une action qui a échoué |
| Faire taire une brique non implémentée | une partie fausse sans qu'on le voie ; le refus est délibéré |
| Brancher `ENTREE_EN_JEU` tout de suite | il faudrait que `piocher()` appelle l'interprète, alors que `Partie` est en dessous de lui |

## Reste à faire

- [ ] **Deux briques refusent encore**, et pour des raisons différentes. *Poser
      une carte du Château* (Roi Yolo) demande de choisir parmi des cartes face
      cachée : le canal de désignation existe, mais l'état n'envoie que
      `tailleChateau`, et l'exposer est une décision de règle autant que d'API.
      *Copier une action Pivoter* (Chapeau magique) désigne une **action** et non
      une carte, et rejouer l'effet d'une autre carte demande de se prémunir
      d'une copie qui se copierait elle-même.
- [ ] **Le Joker ne part pas en partie.** Sa copie fonctionne et elle est testée,
      mais son déclencheur `ENTREE_EN_JEU` n'est pas branché — ce sera au moteur
      de repérer les cartes fraîchement piochées.
- [ ] **Trois révélations sur treize s'inscrivent au journal** au lieu d'agir :
      Booba Brise-Fer, Horde de Gobelins, Sorcière Troll. Toutes trois demandent
      une désignation.
- [ ] **Les jetons Bonus Allié se comptent toujours sans se dépenser.** Cette
      fois ce n'est pas un ticket qui manque, c'est une brique du vocabulaire.
- [ ] **Les Portes et la piste ne montrent toujours rien** (ticket 10). L'état
      porte les ennemis, leurs forces et leurs jetons, mais l'écran affiche des
      cases vides — au point qu'un combat parfaitement fondé donne l'impression
      d'un bug.
- [ ] **Une lecture à confirmer sur Les Jumeaux.** Entre deux exemplaires du même
      type inégalement dotés en jetons, on retient le plus fort — « la force
      d'une seule d'entre elles » ne dit pas laquelle.
