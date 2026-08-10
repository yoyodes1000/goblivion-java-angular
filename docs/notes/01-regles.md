# Ticket 1 — Règles

Note de relecture. Le livrable du ticket est le digest
[`docs/regles/regles-goblivion.md`](../regles/regles-goblivion.md) ; cette note
en extrait ce qui **contraint le moteur**, pour ne pas avoir à relire 20 pages
au moment de coder.

## Ce que les règles imposent au modèle

**1. La force d'une carte se calcule, elle ne se stocke pas.**
Le Soldat vaut 2, 3, 4 ou 5 selon le nombre de Soldats en jeu ; le Joker copie
un Paysan Humain en jeu. Une carte ne peut donc pas porter un `int force`. Il
faut une fonction `force(carte, étatDePartie)`. C'est la contrainte structurante
du modèle de cartes, et elle vient des données autant que des règles — les
champs `§` et `!` dans les fichiers de description ne sont pas des erreurs de
saisie.

**2. L'ordre d'évaluation compte.**
Les Jumeaux ignorent les exemplaires en double, et les exemplaires ignorés
sortent aussi du décompte qui alimente les forces variables : 3 Soldats face aux
Jumeaux valent 2, pas 4. Donc les effets du Boss s'appliquent **avant** le calcul
des forces, jamais après. Un moteur qui somme d'abord et retranche ensuite donne
un mauvais résultat.

**3. « En jeu » est une frontière unique et stricte.**
Seul le Champ de bataille compte. Ni le Château, ni l'Hôpital, ni le plateau
Ennemi, ni le Garde du corps — que le livret exclut explicitement. La même
notion sert au calcul de force, au décompte des Soldats et au ciblage des
actions : une seule fonction, réutilisée partout.

**4. L'action ennemie se déclenche à la révélation, une seule fois.**
Ce n'est pas un état à réinterroger chaque tour, c'est un **événement**. À
l'instant où l'on retourne la carte : révélation pendant la phase Combat →
l'action part ; révélation par une Vision → elle ne part pas. Et ensuite plus
jamais : un ennemi qui survit au combat reste révélé, on le recombat sans que son
action se redéclenche. Seul son jeton Bonus Ennemi continue de peser.

Un simple `révélé: boolean` suffit donc, à condition de traiter la révélation
comme une **transition** et non comme une condition à évaluer. J'avais d'abord
écrit qu'il fallait mémoriser un `tourDeRévélation` : c'était plus compliqué que
nécessaire, parce que je raisonnais en état plutôt qu'en événement.

**5. Un seul visuel par carte Ennemi/Objet.**
Dos commun, recto unique portant les deux moitiés tête-bêche. Côté objet, c'est
la même image pivotée de 180°. Aucun second scan à produire, une rotation CSS
suffit.

**6. La piste d'approche fait 3 cases, pas 4.**
La première case du plateau Ennemi porte la pioche. Un ennemi met 4 avancées à
atteindre les Portes, d'où un premier combat réel au 4e tour. Le plateau
plafonne à 6 ennemis simultanés (3 sur la piste, 3 aux Portes) pour une pioche
de 15.

**7. Quatre points d'entrée pour les effets, pas un.**
Un « exécuter l'action de la carte » unique ne suffira pas. Le moteur d'effets
doit porter :

| Déclencheur | Quand | Exemples |
|---|---|---|
| `Pivoter` | activation volontaire d'une carte en jeu | Boulanger, Grimoire |
| `Testament` | au moment où la carte est détruite | Duc, Traître |
| `Devient Garde du corps` | à l'échange, sur la carte qui entre | Oracle, Patron |
| révélation ennemie | pendant le combat uniquement, une fois | toutes les cartes Ennemi |

Les trois premiers sont lisibles directement dans les données de cartes — les
actions commencent par « Pivoter: », « Testament: » ou « Quand cette carte
devient Garde du Corps: ». Le vocabulaire est déjà là, il reste à le formaliser.

## Décision de nommage

Dans les données de cartes, la clé `bannière` devient **`force`** : c'est ce que
le livret en dit lui-même (« la bannière représente la force au combat », p5), et
le mot parle plus que le nom du dessin.

Pour ne pas entrer en conflit avec la contrainte n°1, on garde deux mots
distincts :

- **`force`** dans les données — la valeur imprimée sur la carte. Vaut `null`
  pour le Soldat et le Joker, dont la carte ne porte pas de nombre.
- **`forceEffective(carte, état)`** dans le moteur — ce que la carte apporte
  réellement ici et maintenant, jetons bonus et effets de Boss compris.

Sans cette séparation, `force` désignerait deux choses différentes et le piège se
refermerait exactement sur les cartes où il fait mal.

## Les points tranchés en discussion

| Question | Réponse | Source |
|---|---|---|
| Le Soldat-récompense compte-t-il comme Soldat aux Portes ? | Non — c'est sa moitié ennemie qui est active. Il ne compte qu'une fois en jeu côté joueur. | table |
| Les Jumeaux contre 3 Soldats | 2 — les exemplaires ignorés sortent du décompte | table |
| Seuil de victoire au combat | inclusif : `force alliée >= force ennemie` | table + p20 |
| Plafond de ressources | aucun — la valeur Roi/Reine est un montant de départ | p4, p7 |
| Seuil de défaite | `ressources <= 0` | p1 |
| Une action ennemie peut-elle repartir ? | Non — une fois par partie, à la révélation | table |
| Apport du Garde du corps au combat | 0, dans les deux phases | table + p11 |
| Échange du Garde du corps | 1× par phase, avec le Champ de bataille ou le terrain d'entraînement | table |

## Ce qui reste hors des règles

Le livret ne contient **aucune donnée de carte** : forces, niveaux, actions et
coûts d'entraînement viennent du matériel physique. Ils sont saisis dans les
fichiers `description-*.txt`, non versionnés. C'est l'objet du ticket suivant.

## Questions de relecture — répondues

1. *Pourquoi `force` ne peut-elle pas être un simple champ du modèle ?* Parce que
   le Soldat et le Joker n'ont pas de valeur imprimée : elle dépend de l'état de
   la partie. D'où la séparation `force` (donnée) / `forceEffective()` (moteur)
   décidée ci-dessus.
2. *Un Gobelin Magicien révélé par une Vision au tour 3 arrive aux Portes au tour
   5 : son action part-elle ?* Non — et elle ne partira jamais.
3. *Le Garde du corps porte une force de 3, combien apporte-t-il au combat ?*
   Zéro. Il n'est pas « En jeu ». Son intérêt est l'échange, pas sa force au
   repos.
