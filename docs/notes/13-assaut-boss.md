# Ticket 13 — L'assaut de Boss

Note de relecture. Ce ticket n'est pas né d'une carte du board mais d'une partie
jouée jusqu'au bout : trois retours d'affilée, tous sur la phase de Boss, dont
un qui rendait la partie imperdable à l'envers — on perdait avant d'avoir joué.

## Le Boss battait le joueur avant qu'il ait pu jouer

À l'entrée de la phase, le Champ de bataille est **vide** : la fin de phase
précédente l'a envoyé à l'Hôpital. `COMBATTRE_BOSS` faisait alors trois choses
d'un seul geste — piocher les cartes du Boss, lancer son action, comparer les
forces. La mesure tombait donc sur la force **imprimée** de cartes que le joueur
n'avait pas eu le droit d'activer.

Reproduit sur les vraies données avant d'écrire quoi que ce soit : défaite au
tour 1, −26 ressources, et les sept cartes piochées encore debout, aucune
pivotée. Le joueur pouvait les activer *après* la comparaison, pour rien.

D'où `ENGAGER_BOSS` puis `RESOUDRE_ASSAUT`. La coupe n'est pas inventée pour
l'occasion : le Combat ordinaire séparait déjà son ouverture de
`RESOUDRE_COMBAT`. Entre les deux, la phase offre ce qu'elle offrait déjà —
pivoter, échanger le Garde du corps, dépenser le pouvoir royal.

Trois garde-fous l'entourent :

- **on n'engage pas deux fois le même assaut** — ce serait piocher deux fois ;
- **on ne résout pas ce qu'on n'a pas engagé** ;
- **on ne quitte pas la phase en laissant une tentative ouverte**, sinon la
  pioche du Boss deviendrait un cadeau : prendre les cartes et s'en aller.

`Partie` retient le **Boss engagé**, pas celui en tête de liste : une carte jouée
entre les deux temps peut ajouter un Boss au paquet, et la tentative doit se
conclure contre celui qu'on affronte.

Le test qui compte rejoue **deux fois la même partie à la même graine** : sans
rien activer entre les deux temps, le Boss résiste ; en pivotant une seule
carte, il tombe. C'est exactement l'écart que le joueur n'avait pas le droit de
produire.

## Un Boss qu'on ne voyait pas

Second retour, dans la foulée : « je ne vois pas la carte du Boss ». Elle
n'existait nulle part à l'écran — le catalogue frontend ne chargeait même pas
`boss.json`. C'était une décision assumée du ticket 9, « les Boss n'ont pas de
scan à montrer tant que la phase n'a pas d'affichage propre », restée en place
faute d'occasion. Les scans, eux, étaient déjà copiés.

Le Boss prend la place des **trois cases des Portes**, comme le joueur l'a
proposé. L'endroit ne change pas de sens : le château brûle quand la phase
s'ouvre, les ennemis qui s'y tenaient sont détruits (§10), et ce qui se dresse
devant les Portes reste ce qu'on affronte.

La carte porte ce qu'il faut lire avant de s'y jeter : force à égaler, action
imprimée, cartes que l'assaut fera piocher, Boss restants. **Force et pioche
viennent du catalogue, pas de l'API** — ce sont des valeurs imprimées qu'aucun
jeton ne modifie (§10.4). C'est le seul chiffre de combat que le frontend lit
sans le tenir du moteur, et il ne se le permet que parce que la règle est fixe.

Une ligne dit aussi où en est l'assaut, engagé ou non. Les deux temps n'avaient
de sens que si l'écran disait lequel est en cours.

## Un ennemi abattu, et non un combat gagné

Troisième retour, et le seul qui corrige une **règle** plutôt qu'une mécanique :
abattre un monstre sur deux dans un combat perdu n'ouvrait pas les cartes 2
épées.

La transcription du ticket 1 disait « après avoir gagné un premier combat », et
le moteur la suivait à la lettre : `premierCombatGagne` n'était marqué que dans
la branche `alliee >= ennemie`. Le propriétaire du livret a tranché autrement —
ce qui compte est d'avoir **vaincu quelque chose**. Un joueur qui abat un
monstre par la répartition a bien gagné une récompense ; lui fermer le marché
n'a pas de sens.

Le drapeau s'appelle donc `premierEnnemiVaincu`, et il se lève dans
`vaincreEnnemi` — le **seul** passage par lequel un ennemi tombe, que le combat
soit gagné ou qu'une répartition l'ait abattu dans une défaite. Le placer là
plutôt qu'aux deux endroits qui appellent cette méthode évite qu'un troisième
chemin, un jour, oublie d'ouvrir la porte.

`docs/regles/regles-goblivion.md` porte désormais la règle juste dans la phrase
elle-même — « après avoir abattu un premier ennemi » — avec une ligne datée
dessous qui rappelle ce qu'elle disait avant. Corriger la transcription plutôt
que l'annoter évite qu'un futur lecteur applique la première lecture sans voir
la seconde.

## Ce que j'ai écarté

| Écarté | Pourquoi |
|---|---|
| Laisser la comparaison dans l'action d'assaut | le joueur perdait avant d'avoir pu activer une carte |
| Un composant Boss séparé des Portes | c'est le même endroit du plateau, qui dit la même chose |
| Envoyer la force du Boss dans l'état | valeur imprimée, jamais modifiée par un jeton : le catalogue suffit |
| Laisser la phrase fautive avec un correctif en dessous | un lecteur pressé appliquerait la première lecture ; la phrase est corrigée, l'ancienne rappelée en note |
| Marquer la porte des 2 épées aux deux appelants | un troisième chemin l'oublierait ; `vaincreEnnemi` est le passage obligé |

## Reste à faire

- [ ] **La phase de Boss n'affiche toujours pas la force alliée** en face de
      celle du Boss. Les deux nombres sont à l'écran — la zone de jeu d'un côté,
      la carte de l'autre — mais pas côte à côte comme au Combat.
- [ ] **Le badge de jeton Bonus Allié ment** quand un passif ignore les jetons
      (Goblinosaurus, Gobelin Pestilent) : il s'affiche et son infobulle annonce
      « déjà compté dans la force », alors que la force l'exclut.
