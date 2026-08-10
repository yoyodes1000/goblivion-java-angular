# Règles de Goblivion — digest d'implémentation (solo)

Reformulation mécanique du livret *Definitive Edition*, écrite pour servir de
référence au moteur de jeu. Ce n'est **pas** une reproduction des règles : ni
texte original, ni illustrations, ni contenu de cartes. Le livret reste la
propriété de Goblivion Games et n'est pas versionné (`.gitignore`).

Périmètre : **solo uniquement**. Les règles coop sont signalées quand elles
éclairent une mécanique, mais ne sont pas à implémenter.

---

## 1. But du jeu

Deckbuilder de défense. On élimine d'abord les troupes ennemies, puis les Boss.

- **Victoire** : tous les Boss vaincus.
- **Défaite** : les ressources tombent à zéro — seuil **inclusif**,
  `ressources <= 0`. Atteindre exactement 0 fait perdre la partie.

Les ressources sont les points de survie. Elles ne servent pas de monnaie
d'achat : on les dépense pour combler un déficit de force.

**Pas de plafond.** La valeur portée par la carte Roi/Reine est le montant de
*départ*, pas une limite : la mise en place dit « Placez ici un nombre de jetons
ressource égal à celui indiqué sur votre carte Roi/Reine. Ceci représente vos
points de survie » (p4), et le symbole Ressource se contente de « Gagnez /
Perdez des jetons ressources de votre plateau Joueur » (p7). Aucun passage
consulté n'introduit de maximum. Les 16 jetons de la boîte sont une contrainte
de matériel, pas une règle — le moteur ne les modélise pas.

---

## 2. Matériel et quantités

| Élément | Quantité | Remarque |
|---|---|---|
| Cartes Bleu | 40 | 25 types distincts, dont Fermier ×12 |
| Cartes Doré | 40 | 12 types distincts |
| Cartes Ennemi/Objet | 23 | recto ennemi, verso objet à 180° |
| Cartes Boss | 11 | |
| Cartes Roi/Reine | 7 | rôles |
| Jeton d'entraînement | 1 | d'où **un seul entraînement par tour** |
| Jetons Ressource | 16 | valeur 1 au recto, 3 au verso |
| Jetons Bonus Allié | 10 | +1 / +2 |
| Jetons Bonus Ennemi | 3 | +1 / +2 |

Le livret précise que ces quantités de jetons sont aussi le maximum autorisé.

### Zones

- **Château** — la pioche du joueur, faces cachées.
- **Hôpital** — la défausse, faces visibles et consultables.
- **Champ de bataille** — les cartes piochées y sont posées ; elles sont « En jeu ».
- **Garde du corps** — un emplacement à part, hors du Champ de bataille.
- **Plateau Ennemi** — la file d'attente des ennemis jusqu'aux Portes du château.

---

## 3. Mise en place

1. 20 cartes Bleu tirées au hasard parmi les 40 → Château, faces cachées.
   Le deck de départ est donc **une moitié aléatoire** des cartes Bleu.
2. Choix d'un rôle Roi/Reine → fixe les ressources de départ et la carte Doré
   qui sert de Garde du corps initial.
3. Les 12 types de cartes Doré forment le marché d'entraînement.
4. Difficulté :

   | Niveau | Boss | Effet supplémentaire |
   |---|---|---|
   | Facile | 3 | on démarre avec 3 jetons Bonus Allié +2 en main |
   | Normal | 4 | — |
   | Difficile | 5 | la partie commence par la phase « L'Ennemi Avance » |

5. Pioche ennemie : 7 cartes au hasard parmi les **2 épées**, puis 8 parmi les
   **1 épée** posées **par-dessus**. Soit **15 ennemis** sur 23, les faibles en
   premier. 8 cartes ne servent pas — la partie n'est jamais la même.
6. Aucune carte en main. Il n'y a **jamais** de main dans ce jeu.

---

## 4. Anatomie des cartes

### Paysan (Bleu et Doré)

Les cartes portant le symbole Paysan représentent l'armée. Deux natures
coexistent : **Humain** et **Objet** — distinction qui compte, plusieurs
actions ne visent que l'une des deux.

- **Bannière** : la force au combat. Peut être négative (jusqu'à −2).
- **Niveau** : poing (0, cartes Bleu) → 1 épée (1) → 2 épées (2).
- **Action** : facultative, déclenchée en pivotant la carte de 90°.
- **Processus d'entraînement** (cartes Doré uniquement) : trois valeurs, voir §6.

### Ennemi / Objet

Attention, ce ne sont **pas** deux faces. La carte a un **dos commun**, et son
unique recto porte les deux moitiés à la fois : l'ennemi en haut, l'objet en
bas et **tête-bêche**. Faire pivoter la carte de 180° ne révèle rien — ça rend
simplement lisible la moitié que l'on veut consulter, en retournant l'autre.

```
   ┌─────────────┐         ┌─────────────┐
   │   ENNEMI    │  180°   │  ǝɯǝuuǝ ↑   │
   ├─────────────┤  ────►  ├─────────────┤
   │  ʇǝظqo ↓    │         │    OBJET    │
   └─────────────┘         └─────────────┘
```

Moitié ennemi : bannière (force), niveau en épées, **nombre de cartes à
piocher** pour l'affronter, et une action.
Moitié objet : bannière et action, comme un Paysan.

Conséquence pour l'implémentation : **un seul visuel par carte**, affiché tel
quel côté ennemi et pivoté de 180° côté objet. Pas de second scan à produire —
ce que confirment les données, qui n'ont qu'un champ `scan carte` et un seul
fichier image par carte.

Un ennemi vaincu est pivoté et rejoint l'Hôpital : la récompense entre dans le
deck du joueur. C'est le moteur du deckbuilding.

### Boss

Deux jeux de valeurs, solo et coop. **La grande valeur en haut est la force du
Boss**, pas des ressources. Un Boss porte aussi un nombre de cartes à piocher
et une action.

---

## 5. Structure d'un tour

Trois phases, répétées jusqu'à la fin de la partie :

1. **Entraînement**
2. **L'Ennemi Avance**
3. **Combat**

À la fin de chaque phase, les cartes du Champ de bataille partent à l'Hôpital.
Le Garde du corps, lui, ne bouge pas.

### Pioche à vide

Quand le Château est vide et qu'il faut piocher : mélanger l'Hôpital et le
replacer face cachée sur le Château. **Conséquence : l'ennemi avance.**
Pendant le combat des Boss, la conséquence devient : **perdre 2 ressources**.

---

## 6. Phase 1 — Entraînement

Poser le jeton d'entraînement sur une carte du marché. Les cartes **1 épée**
sont disponibles dès le départ ; les **2 épées** seulement après avoir gagné un
premier combat.

Le processus d'entraînement se lit de gauche à droite :

| Valeur | Sens |
|---|---|
| A — pioche | nombre de cartes à piocher du Château |
| B — cible | total de force à atteindre |
| C — échange | ce qu'il faut sacrifier : un Paysan **Humain** ou un **Objet** |

Déroulé :

1. Poser le jeton sur la carte désirée.
2. Piocher A cartes.
3. Utiliser les actions des cartes en jeu. Option : échanger le Garde du corps
   (une fois par phase), utiliser le pouvoir Roi/Reine (une fois par partie).
4. Comparer le total de force à la cible B. Si la cible n'est pas atteinte,
   **payer la différence en ressources** pour continuer, ou abandonner
   l'entraînement.
5. Détruire une carte en jeu portant le symbole demandé par C. Sinon,
   abandonner l'entraînement.
6. La nouvelle carte rejoint l'Hôpital, avec les cartes du Champ de bataille.

Une carte **détruite** quitte la partie définitivement — elle ne va pas à
l'Hôpital. C'est le seul moyen d'épurer le deck de ses mauvaises cartes.

L'entraînement n'est jamais obligatoire, et peut être abandonné à l'étape 4 ou 5.

---

## 7. Phase 2 — L'Ennemi Avance

### Géographie du plateau Ennemi

Quatre cases, puis les Portes du château :

```
 [1]        [2]   [3]   [4]        Portes du château
 paquet  →   ·  →  ·  →  ·   →     [ · ][ · ][ · ]
```

La case 1 porte la pioche ennemie. Les cases 2 à 4 forment la piste
d'approche — **trois positions seulement**. Les Portes accueillent au maximum
3 ennemis.

À chaque avancée, tout le monde glisse d'un cran et la case 2 est réalimentée
depuis le paquet :

| Avancée | Mouvements |
|---|---|
| 1re | paquet → 2 |
| 2e | 2 → 3, paquet → 2 |
| 3e | 3 → 4, 2 → 3, paquet → 2 |
| 4e | **4 → Portes**, 3 → 4, 2 → 3, paquet → 2 |

**Un ennemi met donc 4 avancées à atteindre les Portes.** À raison d'une
avancée par tour, le premier ennemi arrive au combat du 4e tour — les trois
premiers tours servent à entraîner et à faire tourner le deck. Toute avancée
supplémentaire (action de carte, Château vide, difficulté Difficile) accélère
d'autant cette horloge.

Les cartes glissent à la queue leu leu, **en comblant toujours le vide derrière
elles**. Une carte face cachée reste cachée, une carte révélée reste révélée.

**Vision** (symbole œil) : certaines actions alliées révèlent une carte Ennemi
aux emplacements marqués du plateau. Conséquence capitale :

> Une carte révélée avant le tour en cours **ne lancera pas son action** au
> combat.

La vision est donc le principal outil pour neutraliser les actions ennemies.

L'action d'un ennemi ne part donc **qu'une seule fois dans la partie**, à
l'instant de sa révélation. Un ennemi qui survit au combat reste révélé : aux
tours suivants, on le recombat sans que son action se redéclenche. Seul son
jeton Bonus Ennemi, acquis une fois pour toutes, continue de peser.

Un ennemi arrive aux Portes **face cachée** : il n'est retourné qu'à l'étape 1
de la phase Combat, qui suit immédiatement. Comme les deux phases s'enchaînent
dans le même tour, retourner la carte dès son arrivée — ce qu'on fait
naturellement à la table — donne le même résultat. La distinction ne devient
visible que pour une carte révélée **plus tôt**, sur la piste d'approche, par
une Vision : celle-là arrive aux Portes déjà retournée, et son action ne partira
jamais. Le moteur doit donc retenir *quand* une carte a été révélée, pas
seulement *si* elle l'est.

**Portes du château** : maximum 3 ennemis. Si un ennemi doit avancer alors que
les Portes sont pleines, il avance quand même et **la prochaine carte qui
serait arrivée est détruite** — on perd sa récompense.

**Si aucun ennemi ne peut avancer pendant cette phase**, on détruit les ennemis
restants aux Portes et on passe au combat des Boss.

---

## 8. Phase 3 — Combat

Le combat est gagné si **force alliée ≥ force ennemie**. Le seuil est inclusif :
égaler la force de l'ennemi suffit à le vaincre.

> 4 de bannières (bonus compris) contre un ennemi de force 4 → vaincu.
> Les mêmes 4 contre un ennemi de force 5 → échec : le joueur perd 1 ressource
> (la différence), et l'ennemi survivant gagne son jeton Bonus Ennemi, qu'il
> conservera au tour suivant.

1. Révéler l'ennemi le plus à gauche aux Portes, piocher le nombre de cartes
   indiqué sur sa carte, puis lancer son action — **seulement s'il est révélé
   ce tour-ci**.
2. Répéter pour chaque ennemi, de gauche à droite.
3. Jouer ses actions. Option : échanger le Garde du corps (1×), pouvoir
   Roi/Reine. Si une pioche déclenche « L'ennemi avance », **revenir à
   l'étape 1** — un nouvel ennemi peut donc entrer en plein combat.
4. Comparer les forces.

**Victoire** : les cartes Ennemi sont retournées à 180° et rejoignent l'Hôpital
avec les cartes du Champ de bataille.

**Défaite** :
- perdre la différence en ressources ;
- **répartir** son total de force sur les ennemis en jeu : tout ennemi dont on
  égale exactement la force est éliminé et donne sa récompense ;
- les survivants gagnent un jeton Bonus Ennemi selon leur niveau — 1 épée → +1,
  2 épées → +2. **Un seul jeton par ennemi**, définitivement acquis.

Un ennemi non éliminé sera à recombattre au tour suivant, avec toute sa force.

---

## 9. Garde du corps

Emplacement à part, qui conserve une carte Paysan ou Objet d'une phase à
l'autre. C'est le principal levier tactique du jeu.

- Il ne part pas à l'Hôpital en fin de phase.
- Il **n'est pas « En jeu »** : sa force ne compte ni en entraînement ni en
  combat. Une carte de force 3 mise au Garde du corps apporte **0**.
- Pour l'utiliser, on l'échange contre une carte en jeu — du **Champ de
  bataille** en phase Combat, du **terrain d'entraînement** en phase
  Entraînement — qui prend sa place.
- **Une seule fois par phase**, mais dans les deux phases.
- On ne peut pas l'échanger contre une carte déjà activée (pivotée à 90°).

L'échange a deux effets immédiats :

1. **La force totale est recalculée** — la carte qui entre en jeu ajoute la
   sienne, celle qui sort retire la sienne.
2. **La carte qui devient Garde du corps peut déclencher une action.** Certaines
   cartes portent un déclencheur « Quand cette carte devient Garde du corps » :
   l'Oracle (Visionner) et le Patron (Piocher 1). C'est une troisième famille de
   déclencheurs, à côté de Pivoter et de Testament.

Le mode d'emploi est donc : mettre de côté une grosse carte pendant une phase
où l'on a de la force en excédent, la ressortir à la phase suivante — et, avec
l'Oracle ou le Patron, se servir de l'échange lui-même comme d'une action.

---

## 10. Combat des Boss

Déclenché quand tous les ennemis sont vaincus, ou quand plus aucun ennemi ne
peut avancer.

1. **Plus d'entraînement.** Le marché est fermé.
2. Le château brûle : **on ne gagne plus aucune ressource**. Et le Château vide
   coûte 2 ressources au lieu de faire avancer l'ennemi.
3. Les Boss sont affrontés **un à la fois**. Un Boss vaincu quitte la partie.
   En cas d'échec, on réessaie — et **le Boss relance son action à chaque
   tentative**.
4. Un Boss ne gagne jamais de jeton bonus.
5. Tous les Boss vaincus → victoire.

Conséquence stratégique : les ressources accumulées avant les Boss sont un
capital non renouvelable.

---

## 11. Symboles

| Symbole | Effet |
|---|---|
| Piocher N | prendre N cartes du Château → Champ de bataille |
| Défausser N | envoyer N autres cartes en jeu à l'Hôpital (jamais elle-même) |
| Ressource ±N | gagner ou perdre N ressources |
| Avancée Ennemie | déclencher un pas de la phase 2 |
| Visionner | révéler une carte Ennemi aux emplacements prévus |
| Jeton Bannière +1 / +2 | poser un bonus de force sur la carte activée |
| Jeton Ennemi +1 / +2 | bonus de force acquis par un ennemi survivant |
| Pivoter | activer l'action de la carte (rotation 90°) |
| Détruire | la carte quitte la partie définitivement |
| Testament | s'active quand la carte est détruite |
| Réactiver | remettre droite une carte pivotée, la rendant réutilisable |

Les jetons Bonus Allié retournent à la banque à la fin de chaque phase. Les
jetons Bonus Ennemi ne reviennent que lorsque l'ennemi est tué.

---

## 12. Précisions du livret (FAQ)

Ces cas particuliers sont autant de tests unitaires à écrire.

- **Égalité** lors d'un ciblage (« le Paysan le plus fort ») : le joueur choisit.
- Les cartes qui **entrent en jeu depuis l'Hôpital** (via une action) sont
  activables immédiatement.
- Une carte dont la **force est ignorée** par un Boss peut quand même lancer son
  action.
- On peut **détruire ou défausser une de ses propres cartes déjà activée**. On ne
  peut pas cibler une carte Ennemi ainsi.
- Le **Chapeau magique** copie l'action d'une carte en jeu, y compris une carte
  déjà activée.
- Le **Chevalier** : la deuxième carte obtenue à l'entraînement ne coûte pas de
  destruction supplémentaire.
- Le **Traître** détruit pendant le combat des Boss : son Testament ne fait rien
  (il n'y a plus d'ennemi à faire avancer).
- Quand la Reine Margot active son pouvoir, l'effet « Quand le Château est
  vide » du plateau Joueur ne se déclenche pas.
- **Soldat** : sa force dépend du nombre de Soldats en jeu — 1→2, 2→3, 3→4,
  4 et plus→5. Chaque Soldat vaut cette valeur.
- Un Soldat existe aussi comme récompense, au dos du Gobelin Trappeur. Il ne
  compte dans le décompte **que lorsqu'il est réellement en jeu** comme carte du
  joueur. Tant que la carte est aux Portes du château, c'est sa moitié ennemie
  qui est active : elle ne vaut pas un Soldat. Le décompte porte sur les cartes
  du Champ de bataille, jamais sur le plateau Ennemi — ni d'ailleurs sur le
  Garde du corps, qui n'est pas « En jeu ».
- **Les jumeaux** (Boss) : on ne compte la force que d'un seul exemplaire de
  chaque doublon en jeu. Les exemplaires en trop sont **ignorés**, ce qui va
  plus loin qu'une simple soustraction : ils sortent aussi du décompte qui sert
  aux forces variables. Trois Soldats face aux Jumeaux ne valent donc pas 4
  (§ pour 3 Soldats, compté une fois) mais **2** — un seul Soldat subsiste au
  décompte, et la formule redescend à son premier palier. Les Jumeaux sont donc
  particulièrement redoutables contre une armée de Soldats.

---

## 13. Ce que le livret ne dit pas

Le détail des cartes n'est pas dans les règles. Les valeurs (force, niveau,
action, coût d'entraînement) viennent du matériel physique et sont saisies
séparément — voir les fichiers de données de cartes, non versionnés.
