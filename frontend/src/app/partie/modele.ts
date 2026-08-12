/**
 * L'état d'une partie tel que le backend l'envoie.
 *
 * Ce fichier est le miroir de `fr.goblivion.api.EtatPartie`. Les deux se
 * correspondent champ pour champ ; le jour où l'un bouge, l'autre doit suivre.
 *
 * Les cartes n'y voyagent que par leur **identifiant** : c'est le catalogue
 * chargé par `Cartes` qui dit ce qu'elles sont. L'API dit où elles sont.
 */

import type { Famille } from '../cartes/modele';
import type { Phase } from '../plateau/phase';

export type Difficulte = 'FACILE' | 'NORMAL' | 'DIFFICILE';

export type Resultat = 'EN_COURS' | 'VICTOIRE' | 'DEFAITE';

/**
 * Les actions du joueur, et rien d'autre.
 *
 * Piocher n'y figure pas : on ne pioche jamais parce qu'on le décide, mais
 * parce qu'un entraînement, un ennemi ou une carte l'exige. C'est une
 * conséquence, que le moteur applique tout seul.
 */
export type TypeAction =
  | 'CHOISIR_ENTRAINEMENT'
  | 'PAYER_DIFFERENCE'
  | 'CONCLURE_ENTRAINEMENT'
  | 'ABANDONNER_ENTRAINEMENT'
  | 'ECHANGER_GARDE_DU_CORPS'
  | 'POUVOIR_ROI_REINE'
  | 'PIVOTER'
  | 'RESOUDRE_COMBAT'
  | 'COMBATTRE_BOSS'
  | 'PHASE_SUIVANTE';

/**
 * Un exemplaire côté joueur.
 *
 * `id` est l'identité de l'exemplaire — celle que les actions désignent. `carte`
 * est l'identifiant du type, qui renvoie au catalogue. Les deux sont
 * indispensables : douze Fermiers partagent le même `carte`, jamais le même
 * `id`, et « détruire le Fermier » ne voudrait rien dire.
 */
export interface CarteVue {
  readonly id: number;
  readonly carte: string;
  readonly famille: Famille;
  /** L'apport **réel**, jetons et forces variables compris — pas la valeur imprimée. */
  readonly force: number;
  readonly pivotee: boolean;
}

/**
 * Un ennemi, sur la piste d'approche ou aux Portes.
 *
 * `carte` vaut `null` tant qu'il est face cachée : le backend ne dit pas ce que
 * le joueur n'a pas le droit de voir. Ce n'est pas une précaution technique,
 * c'est la règle du plateau Ennemi (§7).
 */
export interface EnnemiVue {
  readonly id: number;
  readonly carte: string | null;
  readonly revelee: boolean;
  readonly force: number;
  readonly jetonEnnemi: number;
}

export interface EtatPartie {
  readonly phase: Phase;
  readonly tour: number;
  readonly ressources: number;
  readonly resultat: Resultat;
  readonly difficulte: Difficulte;
  /** `id` de la carte Roi/Reine tirée à la mise en place. */
  readonly role: string;
  readonly gardeDuCorps: CarteVue | null;
  /** Stock restant par type de carte Doré : `id` → exemplaires disponibles. */
  readonly marche: Readonly<Record<string, number>>;
  readonly tailleChateau: number;
  readonly taillePileEnnemie: number;
  readonly champDeBataille: readonly CarteVue[];
  readonly hopital: readonly CarteVue[];
  /** Les cases 2 à 4 dans l'ordre d'approche ; une case vide vaut `null`. */
  readonly piste: readonly (EnnemiVue | null)[];
  readonly portes: readonly EnnemiVue[];
  readonly bossRestants: readonly string[];
  /**
   * Ce que la phase en cours autorise. Le frontend n'a pas à rejouer le tableau
   * des règles pour savoir quels boutons proposer — il lit cette liste.
   */
  readonly actionsPossibles: readonly TypeAction[];
  readonly forceAlliee: number;
  readonly forceEnnemie: number;
  readonly entrainementChoisi: string | null;
  readonly deficitEntrainement: number;
  readonly entrainementTente: boolean;
  readonly combatResolu: boolean;
  readonly premierCombatGagne: boolean;
  readonly gardeDuCorpsEchange: boolean;
  readonly pouvoirRoiReineUtilise: boolean;
  readonly jetonsBonusAllie: number;
  readonly journal: readonly string[];
}

/** Le corps d'une demande d'action, tel que `POST /api/partie/action` l'attend. */
export interface DemandeAction {
  readonly type: TypeAction;
  readonly carteDuMarche?: string;
  readonly carteEnJeu?: number;
  readonly cibles?: readonly number[];
}

/** Le corps d'un refus : le motif est rédigé pour être montré au joueur. */
export interface Refus {
  readonly motif: string;
}
