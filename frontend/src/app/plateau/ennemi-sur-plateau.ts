/**
 * Un ennemi tel qu'il se montre sur le plateau — piste d'approche ou Portes.
 *
 * Le backend n'envoie **pas** l'identité d'une carte encore face cachée : ce
 * n'est pas une précaution technique mais la règle du plateau Ennemi (§7). Ce
 * modèle en tire la conséquence : `image` vaut le dos de la famille tant que la
 * carte n'est pas révélée, et il n'y a jamais de nom à afficher qu'on n'aurait
 * pas le droit de voir.
 */
export interface EnnemiSurPlateau {
  readonly id: number;
  /** Le nom de l'ennemi, ou une mention générique tant qu'il est face cachée. */
  readonly nom: string;
  /** Le scan de la carte, ou le dos de la famille. */
  readonly image: string;
  readonly revelee: boolean;
  /** La force à battre, jeton Bonus Ennemi compris — calculée par le moteur. */
  readonly force: number;
  readonly jetonEnnemi: number;
}
