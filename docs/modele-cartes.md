# Modèle de données des cartes

Les données elles-mêmes — noms, forces, actions — sont du contenu Goblivion
Games et **ne sont pas versionnées**. Ce document décrit leur forme, pour qu'on
puisse les régénérer, les relire ou en changer sans rouvrir les fichiers.

Emplacement local : `data/cartes/`, cinq fichiers, un par famille.
Vérification : `node scripts/valider-cartes.mjs`.

## Principes

**`force`, et non `bannière`.** Le livret dit lui-même que « la bannière
représente la force au combat » (p5). Le mot décrit ce que la valeur fait, pas
le dessin qui la porte.

**`force` est la valeur imprimée, pas l'apport réel.** Deux cartes n'en portent
aucune : le Soldat et le Joker. Pour elles, `force` vaut `null` et
`forceVariable` nomme la règle à appliquer. L'apport réel se calcule dans le
moteur — jetons bonus, effets de Boss — et s'appellera `forceEffective()`. Les
deux mots ne doivent jamais se confondre.

> Invariant, vérifié par le script : une carte a **soit** une `force` numérique,
> **soit** une `forceVariable`. Jamais les deux, jamais ni l'une ni l'autre.

**`id` est le nom du scan sans extension.** Identifiant stable, lisible, et qui
garantit qu'une carte et son image ne peuvent pas se désynchroniser.

**Les énumérations sont en majuscules** — `HUMAIN`, `OBJET`, `SOLDAT`, `JOKER` —
pour se transposer directement en `enum` Java.

**`action` est le texte imprimé, `effets` sa transcription.** Les deux coexistent
et ne se déduisent pas l'un de l'autre : `action` est ce que le joueur lit, mot
pour mot, et le moteur ne s'en sert jamais ; `effets` est ce que le moteur
exécute. Une action absente vaut `null`, jamais `""` ; sa transcription est alors
absente elle aussi.

> Invariant, vérifié par `EffetsDesVraiesDonneesTest` : une carte a `action` et
> `effets`, ou ni l'un ni l'autre. Deux exceptions nommées — le Soldat et son
> jumeau Objet portent un texte dont la règle vit dans `forceVariable`, donc une
> transcription vide.

## Le champ `effets`

Une liste, parce qu'une carte peut agir à plusieurs moments. Chaque entrée porte
un **déclencheur** et un **effet** :

```json
"action": "Pivoter: Défausser 1 et Piocher 1",
"effets": [
  {
    "declencheur": "PIVOTER",
    "effet": {
      "type": "sequence",
      "effets": [
        { "type": "defausser", "nombre": 1 },
        { "type": "piocher", "nombre": 1 }
      ]
    }
  }
]
```

Le `type` de chaque brique est son discriminant : **aucun autre champ ne peut
s'appeler `type`**, sinon l'un des deux est perdu en silence. C'est pourquoi
`obtenir-du-marche` nomme le sien `typeCarte`.

Les huit déclencheurs, les treize cibles, les quatre quantités et les quatre
durées sont énumérés dans `backend/src/main/java/fr/goblivion/effets/`. Le
vocabulaire y est **fermé** : une carte qui ne s'exprime pas avec les briques
existantes n'est pas un cas à contourner, c'est une brique qui manque. L'interface
`Effet` est scellée, donc le compilateur signale tout endroit qui oublierait la
nouvelle.

Trois pièges valent d'être notés, parce qu'ils se ressemblent :

| À ne pas confondre | |
|---|---|
| `melanger-hopital` / `melanger-chateau` | l'un fait entrer l'Hôpital dans le Château, l'autre ne fait que perdre l'ordre connu |
| durée `COMBAT` / `PERMANENTE` | le Gobelin Pestilent ignore les jetons le temps d'un combat, le Goblinosaurus tant qu'il est là |
| cible `UN_…` / `CHAQUE_…` | la première demande une désignation au joueur et suspend l'effet, la seconde se résout seule |

Les cartes royales se déclenchent toutes sur `POUVOIR_ROYAL`, y compris les cinq
qui impriment `Pivoter:` — le geste est de **retourner** la carte royale.

## `bleues.json` — 25 cartes

| Champ | Type | Notes |
|---|---|---|
| `id` | `string` | |
| `nom` | `string` | |
| `type` | `"HUMAIN" \| "OBJET"` | |
| `scan` | `string` | nom de fichier dans `cartes bleues/` |
| `force` | `number \| null` | peut être négative (Mendiant, Voleur…) |
| `forceVariable` | `"SOLDAT" \| "JOKER" \| null` | |
| `niveau` | `number` | toujours 0 pour les Bleues |
| `action` | `string \| null` | |
| `effets` | `EffetCarte[]` | absent si `action` est `null` ; voir plus haut |
| `exemplaires` | `number` | 12 Fermiers, 3 Bûcherons, 3 Épées, 1 sinon |

## `dorees.json` — 12 cartes

Mêmes champs que les Bleues, plus le processus d'entraînement :

| Champ | Type | Notes |
|---|---|---|
| `entrainement.pioche` | `number` | cartes à piocher pour tenter l'entraînement |
| `entrainement.valeur` | `number` | force à réunir |
| `entrainement.sacrifice` | `"HUMAIN" \| "OBJET"` | type de carte à détruire |

## `roi-reines.json` — 7 cartes

| Champ | Type | Notes |
|---|---|---|
| `id`, `nom`, `scan` | `string` | |
| `ressourcesDepart` | `number` | 16 à 21 — montant de départ, **pas un plafond** |
| `gardeDuCorps` | `string` | `id` d'une carte de `dorees.json` |
| `action` | `string` | |

## `boss.json` — 11 cartes

| Champ | Type | Notes |
|---|---|---|
| `id`, `nom`, `scan` | `string` | |
| `action` | `string \| null` | le Dragon Vache n'en a pas |
| `ressourcesSolo` | `number` | force du Boss |
| `cartesAPiocherSolo` | `number` | |
| `ressourcesDeuxJoueurs` | `number` | conservé, **ignoré par le moteur** |
| `cartesAPiocherDeuxJoueurs` | `number` | idem |

Le jeu est modélisé en solo strict. Les valeurs 2 joueurs étaient déjà saisies :
les jeter aurait détruit du travail pour rien, donc elles restent dans le
fichier, clairement nommées, et le moteur ne les lit pas.

## `ennemis-objets.json` — 19 cartes

Une seule carte physique, un seul scan, deux moitiés tête-bêche. D'où un objet
porteur et deux sous-objets, plutôt que deux cartes distinctes :

```json
{
  "id": "...", "scan": "...", "exemplaires": 1,
  "ennemi": { "nom", "niveau", "pioche", "force", "action" },
  "objet":  { "nom", "type", "force", "forceVariable", "action" }
}
```

| Champ | Type | Notes |
|---|---|---|
| `ennemi.niveau` | `number` | 1 ou 2 épées — détermine le jeton Bonus Ennemi |
| `ennemi.pioche` | `number` | cartes à piocher pour l'affronter |
| `ennemi.force` | `number \| null` | |
| `objet.type` | `"HUMAIN" \| "OBJET"` | le Soldat et le Joker sont des Humains |

`exemplaires` porte sur la carte entière : deux Gobelins Assassins, c'est aussi
deux Lames Toxiques.

## État des données

Les 74 cartes sont saisies et le script de validation passe sans anomalie.

Deux valeurs ne venaient pas de la saisie initiale et ont été relevées sur le
matériel physique :

| Carte | Champ | Origine |
|---|---|---|
| Gobelin Nudiste | `ennemi.force` = 1 | valeur absente de la source (`"bannière ennemie":,`) |
| Archer | `action` | guillemet non fermé ; reconstruit d'après le livret p6, puis confirmé sur la carte |
