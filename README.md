# Goblivion — portage personnel (Java + Angular)

Implémentation **personnelle et locale** du jeu de société solo
**Goblivion — Definitive Edition** (deckbuilder de défense de château),
pour y jouer seul contre le système du jeu.

> Projet de fan, **non officiel**, sans affiliation avec Goblivion Games.
> Aucune règle, carte, illustration ni autre contenu sous droits n'est inclus
> dans ce dépôt.

## Pile technique

| Couche | Choix | Version |
|---|---|---|
| Backend | Spring Boot (Maven) | 4.1.0, ciblant Java 21 |
| Frontend | Angular standalone, zoneless | 21 |
| Données de cartes | fichiers JSON lus au démarrage | — |
| Partie en cours | en mémoire | — |

Pas de base de données : le jeu est solo et local, une partie tient en mémoire.
La persistance, si elle devient utile, se fera par sauvegarde JSON.

## Structure

```
goblivion-java-angular/
├── backend/            # Spring Boot — moteur de jeu et API REST
├── frontend/           # Angular — plateau, cartes, interactions
├── docs/
│   ├── regles/         # digest des règles (rédigé, pas le PDF officiel)
│   └── notes/          # notes de relecture, une par ticket
├── cartes */           # scans des cartes — LOCAL, non versionné
└── .gitignore
```

## Démarrer

Backend (port 8080) :

```bash
cd backend && ./mvnw spring-boot:run
```

Frontend (port 4200) :

```bash
cd frontend && npm start
```

## Contenu sous droits — non inclus

Goblivion est une œuvre de **Goblivion Games** (© 2023). Les scans de cartes,
les données de cartes (noms, actions, valeurs) et les livrets de règles sont
**exclus de ce dépôt** et conservés uniquement en local pour le développement.
Le code référence les cartes par identifiant ; il ne les embarque pas.

Pour jouer au vrai jeu : https://www.gobliviongames.com

## Suivi

Les tickets vivent dans l'Organiseur (tableau `goblivion`).
Un ticket = une branche `feature/NN-nom` = une note dans `docs/notes/`.
