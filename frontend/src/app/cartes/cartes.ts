import { httpResource } from '@angular/common/http';
import { Injectable, computed } from '@angular/core';

import type {
  CarteAffichable,
  CarteBleue,
  CarteBoss,
  CarteDoree,
  CarteEnnemiObjet,
  Famille,
  RoiReine,
} from './modele';

/** Où les fichiers atterrissent, une fois `node scripts/copier-medias.mjs` joué. */
const DONNEES = '/cartes/donnees';
const SCANS = '/cartes/scans';

/**
 * Le dos de chaque famille, pour les piles face cachée.
 *
 * Les noms sont relevés sur les fichiers, pas déduits : ils ne suivent pas de
 * règle commune (`dos-bleu`, mais `dos-carte-boss`).
 */
const DOS: Record<Famille, string> = {
  bleues: 'dos-bleu.webp',
  boss: 'dos-carte-boss.webp',
  dorees: 'dos-dore.webp',
  'ennemis-objets': 'dos-ennemi.webp',
  'roi-reines': 'dos-roi-reine.webp',
};

/**
 * L'adresse du scan d'une carte.
 *
 * `scan` est le nom de fichier porté par la donnée, et `id` est ce même nom
 * sans extension : une carte et son image ne peuvent donc pas se désynchroniser.
 */
export function urlScan(famille: Famille, scan: string): string {
  return `${SCANS}/${famille}/${scan}`;
}

export function urlDos(famille: Famille): string {
  return urlScan(famille, DOS[famille]);
}

/** Les quatre familles que le frontend a besoin de résoudre par identifiant. */
export interface CatalogueCartes {
  readonly bleues: readonly CarteBleue[];
  readonly dorees: readonly CarteDoree[];
  readonly roiReines: readonly RoiReine[];
  readonly ennemisObjets: readonly CarteEnnemiObjet[];
}

/**
 * Ce qu'il faut pour montrer une carte, à partir du seul identifiant que le
 * backend envoie.
 *
 * C'est le point de rencontre des deux moitiés du système : le moteur dit *où
 * sont* les cartes, ce catalogue dit *ce qu'elles sont*. Aucun nom ni aucune
 * valeur ne traverse l'API — les données de cartes restent servies en fichiers,
 * lues une fois au démarrage.
 *
 * Pour une carte Ennemi/Objet, c'est le nom de sa moitié **objet** : côté joueur,
 * une carte Ennemi n'existe que par sa récompense, une fois pivotée à 180° (§4).
 * L'ennemi lui-même ne passe jamais par ici — il a son propre affichage.
 *
 * Rend `undefined` plutôt que de lever : une donnée manquante doit laisser
 * l'affichage debout.
 */
export function afficher(
  catalogue: CatalogueCartes,
  famille: Famille,
  id: string,
): CarteAffichable | undefined {
  switch (famille) {
    case 'bleues':
      return depuis(catalogue.bleues, famille, id);
    case 'dorees':
      return depuis(catalogue.dorees, famille, id);
    case 'roi-reines':
      return depuis(catalogue.roiReines, famille, id);
    case 'ennemis-objets': {
      const carte = catalogue.ennemisObjets.find((c) => c.id === id);
      return carte ? { id, nom: carte.objet.nom, scan: carte.scan, famille } : undefined;
    }
    case 'boss':
      // Les Boss ne rejoignent jamais les zones du joueur : ils sont affrontés,
      // pas collectés (§10). Rien à résoudre ici.
      return undefined;
  }
}

function depuis(
  cartes: readonly { id: string; nom: string; scan: string }[],
  famille: Famille,
  id: string,
): CarteAffichable | undefined {
  const carte = cartes.find((c) => c.id === id);
  return carte ? { id: carte.id, nom: carte.nom, scan: carte.scan, famille } : undefined;
}

/**
 * Les données de cartes, lues au démarrage.
 *
 * Elles sont servies en fichiers statiques depuis `frontend/public/cartes/donnees/`,
 * et non par l'API : le backend en a besoin de son côté pour mettre une partie
 * en place, mais les renvoyer sur chaque requête ferait de chaque état une copie
 * du catalogue. L'API ne transporte que des identifiants.
 *
 * `httpResource` est marqué expérimental par Angular : l'API peut bouger d'une
 * version à l'autre. C'est assumé, il n'y a qu'un fichier à corriger. Toute la
 * logique vit dans les fonctions pures ci-dessus, que le service se contente
 * d'appliquer aux données chargées.
 *
 * Les Boss sont chargés comme les autres depuis que la phase de Boss les montre
 * aux Portes. Ils restent hors de {@link CatalogueCartes} : celui-ci sert à
 * résoudre les cartes que le joueur possède, et un Boss ne rejoint jamais ses
 * zones (§10) — il s'affronte, il ne se collectionne pas.
 */
@Injectable({ providedIn: 'root' })
export class Cartes {
  readonly bleues = httpResource<CarteBleue[]>(() => `${DONNEES}/bleues.json`, { defaultValue: [] });
  readonly dorees = httpResource<CarteDoree[]>(() => `${DONNEES}/dorees.json`, { defaultValue: [] });
  readonly roiReines = httpResource<RoiReine[]>(() => `${DONNEES}/roi-reines.json`, { defaultValue: [] });
  readonly ennemisObjets = httpResource<CarteEnnemiObjet[]>(() => `${DONNEES}/ennemis-objets.json`, {
    defaultValue: [],
  });
  readonly boss = httpResource<CarteBoss[]>(() => `${DONNEES}/boss.json`, { defaultValue: [] });

  readonly catalogue = computed<CatalogueCartes>(() => ({
    bleues: this.bleues.value(),
    dorees: this.dorees.value(),
    roiReines: this.roiReines.value(),
    ennemisObjets: this.ennemisObjets.value(),
  }));

  afficher(famille: Famille, id: string): CarteAffichable | undefined {
    return afficher(this.catalogue(), famille, id);
  }

  /**
   * La moitié **ennemi** d'une carte Ennemi/Objet.
   *
   * `afficher()` rend l'autre moitié, et c'est voulu : dans les zones du
   * joueur, une carte Ennemi/Objet est la récompense qu'on a gagnée en
   * l'abattant. Sur la piste et aux Portes, c'est l'ennemi qu'on affronte. Une
   * seule carte physique, deux lectures selon l'endroit (§4) — le backend fait
   * le même partage dans `nomDe`.
   */
  ennemi(id: string): CarteAffichable | undefined {
    const carte = this.ennemisObjets.value().find((c) => c.id === id);
    return carte
      ? { id, nom: carte.ennemi.nom, scan: carte.scan, famille: 'ennemis-objets' }
      : undefined;
  }

  doree(id: string): CarteDoree | undefined {
    return this.dorees.value().find((carte) => carte.id === id);
  }

  roiReine(id: string): RoiReine | undefined {
    return this.roiReines.value().find((carte) => carte.id === id);
  }

  /**
   * Le Boss affronté, résolu par l'identifiant que l'état envoie.
   *
   * À part des autres accès : un Boss n'est pas une `CarteAffichable`, parce que
   * sa force et sa pioche font partie de ce qu'on doit lire avant de s'y jeter.
   */
  bossParId(id: string): CarteBoss | undefined {
    return this.boss.value().find((carte) => carte.id === id);
  }
}
