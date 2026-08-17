# Ticket 10 — UI : ennemis, Portes et hôpital

Note de relecture. Le ticket demandait trois choses : montrer l'avancée des
ennemis sur le plateau, montrer les ennemis aux Portes, et afficher les scans de
l'hôpital au survol d'une zone.

C'est le premier ticket **joué en cours de route**, et c'est ce qui l'a fait
déborder. Trois retours de partie sont arrivés pendant l'écriture et ont changé
son contenu ; une seconde série a suivi, une fois l'écran capable de montrer ce
que le moteur portait. Les retours occupent les deux tiers de la branche, et la
plupart touchent le moteur, pas l'affichage.

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

Vérifié : 167 tests backend, 97 frontend, `valider-cartes.mjs` sans anomalie, et
des parties jouées dans le navigateur contre le vrai jeu de données — ce sont
elles qui ont produit les retours.

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

## La seconde série

Une fois la piste et les Portes visibles, la partie a pu se jouer plus loin — et
c'est là que le vocabulaire du ticket 11 a montré ses trous. Dix correctifs de
plus, presque tous partis d'un retour de partie :

- **Le ciblage n'offrait pas les bonnes cartes.** L'écran proposait l'Hôpital, le
  champ de bataille et la piste sans distinction, et le Champion — dont la cible
  est un ennemi aux Portes — n'était pas jouable du tout. C'est le moteur qui
  répond maintenant : chaque désignation du plan porte ses candidats. Filtrer
  dans le navigateur aurait remis les règles de ciblage dedans.
- **« Un Objet » ne dit pas où chercher.** Le Booba Brise-Fer détruit une carte en
  jeu, le Forgeron en ramène une de l'Hôpital : c'est l'effet qui tranche, pas la
  cible. Mon premier filtre calculait tout sur le champ de bataille et ne
  proposait donc au Forgeron que les Objets qu'il ne peut pas ramener. Le Prêtre
  avait le même défaut.
- **L'Oracle était impossible à poser.** Sa Vision part quand il *devient* Garde
  du corps ; tant que seul `PIVOTER` portait un plan, l'échange partait sans
  réponse et le moteur le refusait en bloc.
- **Le Chevalier ne s'entraînait jamais.** `ENTRAINEMENT` était le seul
  déclencheur du vocabulaire que le moteur n'appelait nulle part : l'effet
  existait dans les données et ne partait pas.
- **La Vision choisissait à la place du joueur** — elle retournait le plus avancé
  des ennemis cachés, qui est justement l'arbitrage intéressant.
- **Le Testament ne partait pas d'un sacrifice.** Sacrifier, c'est détruire (§6),
  mais `conclureEntrainement` sortait la carte sans passer par le chemin de
  destruction : le joueur payait le coût sans toucher la contrepartie.
- **Les jetons Bannière se donnaient d'office** à la carte qui les gagne, alors
  qu'ils se posent où le joueur veut — une erreur de transcription sur cinq
  cartes.
- **Un jeton posé ne s'inscrivait nulle part.** Seul effet muet du jeu : une carte
  piochée saute aux yeux, un point de force se noie dans un total.
- **La carte gagnée s'affichait à l'envers.** Ennemi et objet sont les deux
  moitiés tête-bêche d'une seule carte (§4) ; l'armée du joueur montrait le
  gobelin au lieu de la récompense. Les badges de force ont pris une infobulle
  dans le même geste — le nombre seul était muet, et il vaut d'être là parce
  qu'il peut différer de la valeur imprimée.
- **La fin de partie n'avait pas de sortie** : le panneau annonçait le résultat, et
  seul un rechargement de la page relançait une partie.

Le Chapeau magique s'est ajouté au passage, sans venir d'un retour : copier une
action Pivoter était l'une des deux briques que le ticket 11 laissait refuser.

## La troisième série : la partie est allée jusqu'aux Boss

- **Le jeton Bonus Allié ne se voyait nulle part.** « J'ai l'impression que les
  bonus ne sont pas appliqués » — ils l'étaient, `forceEffective` les compte
  depuis le début et le journal les annonce, mais la carte qui venait de le
  recevoir n'affichait rien. Un total ne dit pas d'où il vient. Les ennemis
  portaient déjà leur jeton aux Portes ; les cartes du joueur ont maintenant le
  même badge, et `CarteVue` transporte le jeton **en plus** de la force pour que
  l'écran n'ait rien à recalculer.
- **Le Boss battait le joueur avant qu'il ait pu jouer.** À l'entrée de la phase,
  le Champ de bataille est vide — la fin de phase précédente l'a envoyé à
  l'Hôpital. `COMBATTRE_BOSS` piochait alors les cartes du Boss, lançait son
  action **et comparait les forces** d'un seul geste : la mesure tombait sur la
  force imprimée de cartes que le joueur n'avait pas eu le droit d'activer.
  Reproduit sur les vraies données : défaite au tour 1, −26 ressources, et les
  sept cartes piochées encore debout, aucune pivotée.

L'assaut se joue donc en deux temps, `ENGAGER_BOSS` puis `RESOUDRE_ASSAUT`. La
coupe n'est pas inventée pour l'occasion : le Combat ordinaire l'avait déjà,
entre son ouverture et `RESOUDRE_COMBAT`. Trois garde-fous l'entourent — on
n'engage pas deux fois le même assaut, on ne résout pas ce qu'on n'a pas engagé,
et on ne quitte pas la phase en laissant une tentative ouverte, sinon la pioche
du Boss deviendrait un cadeau.

Le test qui compte rejoue **deux fois la même partie à la même graine** : sans
rien activer entre les deux temps, le Boss résiste ; en pivotant une seule
carte, il tombe. C'est exactement l'écart que le joueur n'avait pas le droit de
produire.

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

- [ ] **La phase de Boss n'a toujours aucun affichage.** Le Boss affronté n'est
      ni montré ni nommé, et sa force n'arrive pas au frontend — le catalogue ne
      charge pas les Boss, faute d'écran pour eux. Depuis que l'assaut se joue en
      deux temps, le manque se voit : le joueur prépare son armée, puis résout
      sans savoir de combien il avait besoin.
- [ ] **Poser une carte du Château refuse encore** (Roi Yolo, ticket 11) :
      désigner parmi des faces cachées demande d'exposer autre chose que
      `tailleChateau`, et c'est une décision de règle autant que d'API.
- [ ] **Le Joker attend toujours son déclencheur** `ENTREE_EN_JEU` (ticket 11).
      Sa copie fonctionne et elle est testée ; c'est au moteur de repérer les
      cartes fraîchement piochées.
- [ ] **Les jetons Bonus Allié de départ se comptent sans se dépenser** (ticket
      11) : `Partie` les reçoit de la difficulté, et rien ne les retire.
- [ ] **Les trois cartes à désignation ne sont pas sorties du paquet** pendant
      les vérifications : Booba Brise-Fer, Horde de Gobelins, Sorcière Troll. Le
      cycle est couvert par des tests déterministes des deux côtés, mais jamais
      encore vu en jeu.
