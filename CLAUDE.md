# Conventions du dépôt

Portage personnel de **Goblivion — Definitive Edition** (Spring Boot + Angular).
Voir [README.md](README.md) pour la pile et le démarrage.

## Règle d'écriture

Trois zones, du plus libre au plus verrouillé.

**Libre, sans demander :**

- lire n'importe quoi, n'importe où ;
- créer un fichier ; ajouter une section, un bloc, des lignes ;
- lancer les tests, le build, `node scripts/valider-cartes.mjs` ;
- `git add`, `git commit`, créer une branche ;
- pousser sur `feature/*` et `dev`.
- retoucher ses propres lignes, dans un fichier créé pendant le ticket en cours.

**Toujours demander avant :**

- supprimer ou réécrire du contenu **préexistant** — y compris remplacer une ligne
  déjà là, pas seulement effacer un bloc ;
- supprimer ou renommer un fichier, une branche ;
- `git reset`, `git rebase`, `push --force`, `--amend` sur un commit déjà poussé,
  `git revert`, `git clean` ;
- retirer ou rétrograder une dépendance ;
- **toucher au `.gitignore`**, en particulier relâcher une règle : les scans et les
  données de cartes sont du contenu Goblivion Games, le dépôt est **public**, une
  règle desserrée par mégarde est une fuite ;
- modifier `.github/workflows/`.

**Réservé au propriétaire du dépôt :**

- promouvoir vers `staging` et vers `main` ;
- ouvrir et fusionner les Pull Requests.

`.claude/settings.json` double une partie de ces règles en confirmations
automatiques. Il n'attrape que ce qui passe par un motif de commande : une
suppression de lignes au fil d'une édition lui échappe forcément. Le garde-fou
réel est la règle ci-dessus, pas le fichier.

## Flux Git

Quatre niveaux, une seule histoire linéaire.

```
feature/NN-nom ──PR──> dev ──ff──> staging ──ff──> main
                        ▲            ▲               ▲
                        CI verte   tests manuels   production validée
```

| Branche | Rôle |
|---|---|
| `main` | Production. Ce qui a été joué et validé. Protégée. |
| `staging` | Version candidate, en cours de test manuel. Protégée. |
| `dev` | Intégration. Toujours verte : la CI garde l'entrée. |
| `feature/NN-nom` | Un ticket, une branche, une note dans `docs/notes/`. |

**Promotion en fast-forward uniquement.** Les trois branches longues sont des
étiquettes posées sur une seule histoire. Aucun merge de réconciliation, aucune
divergence à rattraper, et `git log main..dev` dit exactement ce qui attend d'être
validé. La contrepartie : on ne commite **jamais** directement sur `staging` ni sur
`main`, sinon le fast-forward casse.

`dev` est la **branche par défaut du dépôt** : une PR ouverte sans `--base` la
vise d'elle-même, et un clone frais atterrit dessus. `main` reste la production.

Développer un ticket :

```bash
git checkout dev && git pull --ff-only && git checkout -b feature/NN-nom
```

Puis PR vers `dev`. La CI (`.github/workflows/ci.yml`) tourne sur la PR : backend
`mvnw test`, frontend `npm ci` + tests + build.

À la fusion, GitHub supprime la branche **distante** tout seul (`delete_branch_on_merge`).
La copie **locale** survit, elle : `git branch -d feature/NN-nom && git fetch --prune`.

Promouvoir vers `staging`, une fois la CI verte :

```bash
git push origin dev:staging
```

Promouvoir vers `main`, une fois les tests manuels concluants :

```bash
git push origin staging:main
```

Ces deux pushes échouent d'eux-mêmes s'ils ne sont pas des fast-forward — c'est
le filet : un refus signifie que quelqu'un a commité directement sur la cible.

Un `Everything up-to-date` n'est jamais un échec : c'est une promotion déjà faite.
Un vrai refus s'affiche en `! [rejected]`.

### Marquer la version

Toute promotion vers `main` se conclut par un tag. Sans lui, `main` avance sans
que rien ne distingue une version d'une autre, et l'historique perd la seule
information que `main` est censée porter : ce qui a été joué et validé, et quand.

```bash
git tag -a v0.2.0 -m "Ce que cette version apporte"
git push origin v0.2.0
```

Tag **annoté** (`-a`), pas léger : il porte un auteur, une date et un message,
là où un tag léger n'est qu'un pointeur anonyme. Et il faut le **pousser
explicitement** — un `git push` ordinaire n'emporte pas les tags.

Numérotation :

| Forme | Quand |
|---|---|
| `v0.x.0` | tant que le jeu n'est pas jouable de bout en bout — chaque promotion apportant du contenu |
| `v0.x.y` | promotion de correction seule, sans apport |
| `v1.0.0` | première partie complète jouable, du début à la victoire ou la défaite |

`git tag -n` liste les versions avec leur message, `git describe` situe n'importe
quel commit par rapport à la dernière.

**Pas de chemin `hotfix`.** À un seul développeur sur un jeu local, une branche
partant de `main` casserait la linéarité pour rien. Un bug urgent se corrige sur
`dev` et se promeut dans la foulée.

## Validation des cartes

`scripts/valider-cartes.mjs` ne tourne **pas** en CI : il lit `data/cartes/`, qui
est hors dépôt. C'est un garde-fou local, à jouer avant de pousser dès que les
données de cartes bougent.

```bash
node scripts/valider-cartes.mjs
```

## Conventions d'écriture

- Documentation et commentaires en français.
- Messages de commit en français, sans accents, format `type(portee): sujet`.
- Le frontend a ses propres conventions Angular dans
  [frontend/.claude/CLAUDE.md](frontend/.claude/CLAUDE.md).
- Le backend cible **Java 21** délibérément, même si le JDK local est plus récent.
