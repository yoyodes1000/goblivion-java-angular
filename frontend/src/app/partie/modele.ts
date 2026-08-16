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
  | 'REPONDRE_DESIGNATION'
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
/**
 * Ce qu'une action de carte réclamera, annoncé avant d'être jouée.
 *
 * Le frontend ne déduit pas ce qu'il faut demander : il le lit. Rejouer le
 * vocabulaire des effets côté navigateur reviendrait à en tenir une seconde
 * implémentation, qui finirait par ne plus être d'accord avec la première.
 *
 * `designations` est **ordonné** : le moteur les consomme dans cet ordre, donc
 * les renvoyer autrement ferait détruire la mauvaise carte.
 */
/**
 * Une question à poser au joueur.
 *
 * `parType` sépare deux choses que rien d'autre ne distingue : désigner un
 * **exemplaire** posé sur la table, qui a une identité, et choisir un **type**
 * de carte au Marché, qui n'est pas encore en jeu et n'en a donc pas. Les deux
 * réponses voyagent par des canaux séparés jusqu'au moteur.
 */
export interface Designation {
  readonly libelle: string;
  readonly parType: boolean;
  /**
   * Les exemplaires que cette cible accepte, calculés par le moteur.
   *
   * L'écran n'a qu'à filtrer sur cette liste. La déduire lui-même — « un Objet,
   * donc les cartes de type OBJET en jeu » — reviendrait à tenir une seconde
   * version des règles de ciblage, qui finirait par proposer une carte que
   * l'interprète refuse, ou par en cacher une qu'il accepte.
   */
  readonly candidats: readonly number[];
}

/**
 * Une branche d'un « ou », avec ce qu'elle réclame en propre.
 *
 * « Piocher 1 ou Visionner » ne demande rien dans un cas, un ennemi à retourner
 * dans l'autre. Mettre les deux en commun ferait poser une question sans objet
 * à qui choisit de piocher.
 */
export interface Branche {
  readonly libelle: string;
  readonly designations: readonly Designation[];
}

export interface PlanDeCiblage {
  readonly designations: readonly Designation[];
  /** Les branches d'un « ou » — vide s'il n'y en a pas. */
  readonly options: readonly Branche[];
}

export interface CarteVue {
  readonly id: number;
  readonly carte: string;
  readonly famille: Famille;
  /** L'apport **réel**, jetons et forces variables compris — pas la valeur imprimée. */
  readonly force: number;
  /**
   * Le jeton Bonus Allié posé sur l'exemplaire, `0` s'il n'y en a pas.
   *
   * Déjà compté dans `force`, et pourtant envoyé à part : un total ne dit pas
   * d'où il vient. Sans lui, le joueur qui vient de désigner à qui donner son
   * jeton voit un nombre bouger sans savoir que c'est le sien.
   */
  readonly jetonBanniere: number;
  readonly pivotee: boolean;
  /**
   * Le type que la carte joue pour cette phase, `null` si elle est elle-même.
   *
   * Le Joker, et lui seul : il prend toutes les caractéristiques d'un Humain
   * jusqu'à la fin de la phase. L'affichage doit suivre, sinon la table
   * montrerait un Joker là où le moteur voit un Fermier.
   */
  readonly copie: string | null;
  readonly plan: PlanDeCiblage;
  /**
   * Ce que la carte réclamera si elle **devient Garde du corps**.
   *
   * L'Oracle visionne à ce moment-là, le Prêtre ramène un Humain de l'Hôpital.
   * Les deux partent d'un clic du joueur, donc leurs désignations peuvent
   * voyager avec la demande — mais il faut les lui demander avant.
   */
  readonly planEchange: PlanDeCiblage;
  /**
   * Vrai si la carte a quelque chose à déclencher quand on la pivote.
   *
   * Un plan vide ne suffit pas à le dire : le Boulanger agit sans rien
   * demander, un Fermier n'agit pas du tout, et les deux ont un plan vide.
   * Proposer « Pivoter » sur une carte sans action serait offrir un bouton qui
   * ne fait rien.
   */
  readonly agitAuPivot: boolean;
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

/**
 * Une question que le moteur pose, sans que le joueur l'ait demandée.
 *
 * Un ennemi révélé exige de désigner une carte, et le joueur ne pouvait pas le
 * prévoir : il n'avait rien à joindre à sa demande. Tant que cette question est
 * là, le moteur refuse toute autre action — l'écran doit donc la poser avant
 * quoi que ce soit d'autre.
 */
export interface DesignationAttendue {
  readonly source: string;
  readonly plan: PlanDeCiblage;
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
  /** La question en suspens, `null` s'il n'y en a pas. */
  readonly designationAttendue: DesignationAttendue | null;
  readonly journal: readonly string[];
}

/** Le corps d'une demande d'action, tel que `POST /api/partie/action` l'attend. */
export interface DemandeAction {
  readonly type: TypeAction;
  readonly carteDuMarche?: string;
  readonly carteEnJeu?: number;
  /** Les exemplaires désignés, dans l'ordre où `PlanDeCiblage` les annonce. */
  readonly cibles?: readonly number[];
  /** Les branches retenues face à un « ou », dans le même ordre. */
  readonly options?: readonly number[];
  /** Les **types** de carte choisis — le Marché, qui n'a pas d'exemplaires en jeu. */
  readonly types?: readonly string[];
}

/** Le corps d'un refus : le motif est rédigé pour être montré au joueur. */
export interface Refus {
  readonly motif: string;
}
