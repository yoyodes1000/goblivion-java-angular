import { httpResource } from '@angular/common/http';
import { Injectable, computed } from '@angular/core';

import type { CarteDoree, CarteEnnemiObjet, Famille, RoiReine } from './modele';

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

/**
 * Le Garde du corps initial que désigne une carte Roi/Reine (§3 des règles).
 *
 * Le lien se fait par `id`, et l'`id` visé est celui d'une carte Doré. Rien ne
 * garantit côté données que la cible existe — d'où le `undefined` possible,
 * plutôt qu'une exception qui ferait tomber l'affichage entier.
 */
export function trouverGardeDuCorps(
  dorees: readonly CarteDoree[],
  roiReine: RoiReine | undefined,
): CarteDoree | undefined {
  if (!roiReine) return undefined;
  return dorees.find((carte) => carte.id === roiReine.gardeDuCorps);
}

/**
 * La taille de la pile Ennemi.
 *
 * Ce n'est pas le nombre de cartes distinctes : `exemplaires` porte sur la carte
 * physique, et certaines existent en plusieurs copies.
 */
export function compterEnnemis(cartes: readonly CarteEnnemiObjet[]): number {
  return cartes.reduce((total, carte) => total + carte.exemplaires, 0);
}

/**
 * Les données de cartes, lues au démarrage.
 *
 * En attendant le backend, elles sont servies en fichiers statiques depuis
 * `frontend/public/cartes/donnees/`. Le jour où Spring les servira, seules les
 * adresses ci-dessus changeront — les composants ne verront rien.
 *
 * `httpResource` est marqué expérimental par Angular : l'API peut bouger d'une
 * version à l'autre. C'est assumé, il n'y a qu'un fichier à corriger. Toute la
 * logique vit dans les fonctions pures ci-dessus, que le service se contente
 * d'appliquer aux données chargées.
 */
@Injectable({ providedIn: 'root' })
export class Cartes {
  readonly dorees = httpResource<CarteDoree[]>(() => `${DONNEES}/dorees.json`, { defaultValue: [] });
  readonly roiReines = httpResource<RoiReine[]>(() => `${DONNEES}/roi-reines.json`, { defaultValue: [] });
  readonly ennemisObjets = httpResource<CarteEnnemiObjet[]>(() => `${DONNEES}/ennemis-objets.json`, {
    defaultValue: [],
  });

  readonly nombreEnnemis = computed(() => compterEnnemis(this.ennemisObjets.value()));

  gardeDuCorpsDe(roiReine: RoiReine | undefined): CarteDoree | undefined {
    return trouverGardeDuCorps(this.dorees.value(), roiReine);
  }
}
