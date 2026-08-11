import { httpResource } from '@angular/common/http';
import { Injectable } from '@angular/core';

import type { CarteAffichable, CarteBleue, CarteDoree, Famille, RoiReine } from './modele';

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

/** Réduit une carte Bleue à ce qu'il faut pour la montrer. */
export function afficherBleue(carte: CarteBleue): CarteAffichable {
  return { id: carte.id, nom: carte.nom, scan: carte.scan, famille: 'bleues' };
}

/**
 * La pile Ennemi d'une partie : **15 cartes**, et non les 23 qui existent.
 *
 * La mise en place en tire 7 parmi les 2 épées, puis 8 parmi les 1 épée posées
 * par-dessus (§3). Huit cartes ne servent pas — c'est ce qui fait qu'une partie
 * n'est jamais la même. Lesquelles, c'est un tirage : affaire du moteur.
 */
export const TAILLE_PILE_ENNEMIE = 15;

/**
 * Le Château au départ : **20 cartes Bleu** tirées au hasard parmi les 40 (§3).
 * Là encore, lesquelles est un tirage que le moteur fera.
 */
export const TAILLE_CHATEAU_DEPART = 20;

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
 *
 * Seules les trois familles réellement affichées sont chargées. Les Ennemis et
 * les Boss viendront quand il y aura quelque chose à en montrer — ticket 10.
 */
@Injectable({ providedIn: 'root' })
export class Cartes {
  readonly bleues = httpResource<CarteBleue[]>(() => `${DONNEES}/bleues.json`, { defaultValue: [] });
  readonly dorees = httpResource<CarteDoree[]>(() => `${DONNEES}/dorees.json`, { defaultValue: [] });
  readonly roiReines = httpResource<RoiReine[]>(() => `${DONNEES}/roi-reines.json`, { defaultValue: [] });

  gardeDuCorpsDe(roiReine: RoiReine | undefined): CarteDoree | undefined {
    return trouverGardeDuCorps(this.dorees.value(), roiReine);
  }
}
