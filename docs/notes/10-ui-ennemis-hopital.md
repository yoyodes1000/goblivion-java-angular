# Ticket 10 — UI : ennemis, Portes et hôpital

Note de relecture. Le ticket demandait trois choses : montrer l'avancée des
ennemis sur le plateau, montrer les ennemis aux Portes, et afficher les scans de
l'hôpital au survol d'une zone.

C'est le premier ticket **joué en cours de route**. Trois retours de partie sont
arrivés pendant l'écriture et ont changé son contenu : ils occupent la moitié de
la branche, et deux d'entre eux touchent le moteur.

## Ce qui existe maintenant

```
frontend/src/app/
├── plateau/
│   ├── ennemi-sur-plateau.ts    le modele commun aux deux zones
│   ├── plateau-avancee/         la piste, trois cases
│   └── portes-chateau/          les Portes, trois places
└── partie/
    └── repartition/             l'arbitrage d'un combat perdu
```

Côté moteur, un état d'attente : `EffetEnAttente`, `TypeAction.REPONDRE_DESIGNATION`
et le champ `designationAttendue` de l'API.

Vérifié : 150 tests backend, 89 frontend, et une partie jouée dans le navigateur
contre le vrai jeu de données — c'est elle qui a produit les trois retours.

## L'hôpital était déjà fait

Rien n'a été écrit pour le troisième point. `chateau-hopital` faisait déjà
exactement ce que le ticket décrit : survol de la case, fenêtre avec les scans,
et en prime le focus clavier et Échap. Il a été construit au ticket 9, avant que
le moteur existe, et la checklist le listait encore.

Le lire avant de l'écrire a évité de refaire — et de refaire moins bien, puisque
la version en place sépare **survol et focus en deux signaux** réunis par un
`computed` : un seul signal partagé aurait vu le `mouseleave` refermer une
fenêtre ouverte au clavier.

## Deux moitiés, deux lectures

Le catalogue frontend ne savait pas nommer un ennemi. `afficher()` rend la moitié
**objet** d'une carte Ennemi/Objet — ce qui est juste dans les zones du joueur,
où cette carte est la récompense gagnée en l'abattant, et faux sur la piste où
c'est l'ennemi qu'on affronte.

Le backend faisait déjà ce partage dans `nomDe` ; le frontend l'ignorait faute
d'en avoir eu besoin. D'où `Cartes.ennemi(id)`, à côté de `afficher()`.

## La position est l'information

Une case vide de la piste reste dessinée à sa place. Filtrer les vides pour
compacter l'affichage perdrait la **distance aux Portes**, qui est tout l'enjeu
du plateau d'approche : un ennemi en case 3 arrive au prochain tour, un ennemi en
case 1 dans trois tours. Un test le verrouille.

De même, une carte face cachée ne dit ni son nom ni sa force. Le backend ne les
envoie pas — c'est la règle du plateau Ennemi (§7), pas une précaution technique
— et l'affichage ne fait qu'en tirer la conséquence.

## Le bug qui ne pouvait pas se voir en test

Un dos de carte prenait tout l'écran, dès le premier ennemi sur la piste.

J'avais ajouté les éléments `.avancee__carte` et `.portes__carte` dans les
templates **sans écrire leur CSS**. Une image `ngSrc` en `fill` se positionne sur
le premier ancêtre *positionné* : sans `position: relative` sur le parent, elle
remonte jusqu'à la fenêtre et la remplit.

Mes tests vérifient le DOM, pas la mise en page — ils ne pouvaient pas l'attraper.
C'est le joueur qui l'a vu. À retenir pour la suite : un composant qui pose une
image en `fill` doit poser son conteneur dans le même geste.

## Les trois retours de partie

### « Après un sacrifice, on ne peut plus changer le garde du corps »

Ce n'était pas le sacrifice. Une carte **activée** ne peut ni repivoter ni
devenir Garde du corps (§9), et l'écran faisait disparaître ses deux boutons sans
un mot — ce qui se lit comme une panne. Comme on active justement des cartes pour
atteindre la cible d'entraînement, les deux se suivent dans une partie.

La règle était juste : un test backend rejoue la séquence complète et montre que
le moteur accepte toujours l'échange après un sacrifice. Il reste comme
garde-fou. Le correctif est ailleurs — la carte annonce son état.

**Leçon :** cacher un bouton parce que l'action est refusée n'est pas neutre. Le
ticket 12 avait posé « le bouton n'existe pas plutôt que d'exister et de se faire
refuser » ; c'est vrai quand la raison est évidente, faux quand elle ne l'est pas.

### « On devrait pouvoir répartir sa force sur les monstres »

Le moteur le faisait déjà depuis le ticket 12. C'est l'écran qui envoyait
`RESOUDRE_COMBAT` **sans aucune cible** : personne ne tombait, et chaque
survivant empochait son jeton. Une décision prise à la place du joueur, et la
pire des trois possibles.

D'où un panneau de répartition — une sélection à **budget**, et non une suite de
questions comme le ciblage : le joueur arbitre, il doit voir en même temps ce
qu'il dépense et ce qui lui reste. Un ennemi trop cher pour le reliquat est
montré désactivé plutôt que caché ; savoir ce qu'on ne peut pas s'offrir fait
partie de l'arbitrage.

### « Quand un monstre exige un sacrifice, c'est au joueur de choisir »

Celui-là rouvre une décision du ticket 11. On avait choisi « noter et passer »
pour les effets déclenchés par le moteur, faute de pouvoir réclamer une
désignation au milieu d'un tour. Ce retour la tranche autrement : un jeu qui
décide à la place du joueur quel paysan meurt ne joue pas la même partie.

D'où le **seul endroit du moteur qui suspend quelque chose**. Un effet de
révélation qui réclame une désignation se met en attente ; tant qu'elle tient,
`REPONDRE_DESIGNATION` est la seule action acceptée.

Deux garde-fous :

- **Une réponse invalide ne consomme pas la question.** La double passe de
  l'interprète garantit que rien n'a bougé — le joueur peut se tromper.
- **Une défaite efface les questions en suspens**, sinon l'écran de fin resterait
  coincé derrière une question à laquelle le moteur refuserait de répondre.

## Un test qui a corrigé une conception

Ma première version rendait `REPONDRE_DESIGNATION` permise dans toutes les
phases. Un test du ticket 12 est tombé : « la phase d'Avancée n'accepte aucune
action » (§7).

Plutôt que de réécrire ce test, j'ai corrigé la conception. `permisesEn(phase)`
dit ce que la phase **offre** au joueur ; répondre n'est pas un choix qu'on prend
dans un menu, c'est la suite obligée d'un effet déjà parti. L'action reste donc
acceptée par le moteur mais absente de cette liste, et la règle du ticket 12
tient intacte.

C'est la deuxième fois dans ce portage qu'un test du ticket 12 rattrape une
erreur de conception — la première était l'assaut de Boss qui repart à chaque
tentative.

## Ce que j'ai écarté

| Écarté | Pourquoi |
|---|---|
| Refaire l'hôpital au survol | il existait déjà, et en mieux que ce que j'aurais réécrit |
| Compacter la piste en filtrant les cases vides | la position dit la distance aux Portes |
| Afficher le nom d'un ennemi face cachée | le backend ne l'envoie pas, et c'est la règle |
| Recalculer la force ennemie à l'écran | ce serait réimplémenter la règle des jetons |
| Ouvrir la répartition sur un combat gagné | rien à arbitrer, tous les ennemis tombent |
| Un « renoncer » sur une question du moteur | il refuse tout le reste : renoncer ne mènerait nulle part |
| Choisir à la place du joueur quel paysan meurt | ce n'est pas la même partie |

## Reste à faire

- [ ] **Le badge de force est muet.** Un « 2 » en haut à droite d'une carte
      ennemie sans rien pour le nommer — la question s'est posée en jouant. Il
      dit la force à battre, jeton compris, et ne vaut d'être là que parce qu'il
      peut différer de la valeur imprimée sur le scan.
- [ ] **Les trois cartes à désignation ne sont pas sorties du paquet** pendant
      les vérifications : Booba Brise-Fer, Horde de Gobelins, Sorcière Troll. Le
      cycle est couvert par des tests déterministes des deux côtés, mais jamais
      encore vu en jeu.
- [ ] **Le Joker attend toujours son déclencheur** `ENTREE_EN_JEU` (ticket 11).
- [ ] **Deux briques d'effets refusent encore** : poser une carte du Château et
      copier une action Pivoter (ticket 11).
