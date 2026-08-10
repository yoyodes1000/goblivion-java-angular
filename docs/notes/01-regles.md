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

**4. Une révélation porte une date, pas un booléen.**
Une action ennemie ne part que si l'ennemi est révélé **pendant le combat en
cours**. Révélé plus tôt par une Vision, il arrive aux Portes déjà retourné et
son action ne partira jamais. `révélée: boolean` ne suffit pas, il faut
`tourDeRévélation`.

**5. Un seul visuel par carte Ennemi/Objet.**
Dos commun, recto unique portant les deux moitiés tête-bêche. Côté objet, c'est
la même image pivotée de 180°. Aucun second scan à produire, une rotation CSS
suffit.

**6. La piste d'approche fait 3 cases, pas 4.**
La première case du plateau Ennemi porte la pioche. Un ennemi met 4 avancées à
atteindre les Portes, d'où un premier combat réel au 4e tour. Le plateau
plafonne à 6 ennemis simultanés (3 sur la piste, 3 aux Portes) pour une pioche
de 15.

## Les quatre points tranchés en discussion

| Question | Réponse | Source |
|---|---|---|
| Le Soldat-récompense compte-t-il comme Soldat aux Portes ? | Non — c'est sa moitié ennemie qui est active. Il ne compte qu'une fois en jeu côté joueur. | table |
| Les Jumeaux contre 3 Soldats | 2 — les exemplaires ignorés sortent du décompte | table |
| Seuil de victoire au combat | inclusif : `force alliée >= force ennemie` | table + p20 |
| Plafond de ressources | aucun — la valeur Roi/Reine est un montant de départ | p4, p7 |
| Seuil de défaite | `ressources <= 0` | p1 |

## Ce qui reste hors des règles

Le livret ne contient **aucune donnée de carte** : forces, niveaux, actions et
coûts d'entraînement viennent du matériel physique. Ils sont saisis dans les
fichiers `description-*.txt`, non versionnés. C'est l'objet du ticket suivant.

## Trois questions pour vérifier

1. Pourquoi `force` ne peut-elle pas être un champ du modèle de carte ?
2. Un Gobelin Magicien est révélé par une Vision au tour 3, et arrive aux Portes
   au tour 5. Son action se déclenche-t-elle ?
3. Le Garde du corps porte une bannière de 3. Combien apporte-t-il au combat ?
