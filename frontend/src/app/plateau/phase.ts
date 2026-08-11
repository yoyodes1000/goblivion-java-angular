/**
 * Les phases d'un tour.
 *
 * L'ordre des tours imprimé sur le plateau château est Entraînement →
 * L'Ennemi Avance → Combat (§5 des règles). Le Combat de Boss est le quatrième
 * état : il arrive en fin de partie et ferme le marché d'entraînement (§10).
 */
export type Phase = 'entrainement' | 'avancee' | 'combat' | 'boss';

/** Les phases dans l'ordre où elles s'enchaînent. */
export const PHASES = ['entrainement', 'avancee', 'combat', 'boss'] as const satisfies readonly Phase[];

export interface LibellesPhase {
  /** Ce que le bandeau annonce. */
  readonly bandeau: string;
  /**
   * Le nom que porte la zone de jeu pendant cette phase. Le même espace
   * s'appelle « terrain d'entraînement » en Entraînement et « champ de
   * bataille » le reste du tour — c'est la distinction du §9, qui décrit
   * l'échange du Garde du corps contre une carte de l'un ou de l'autre.
   */
  readonly zoneDeJeu: string;
}

export const LIBELLES: Readonly<Record<Phase, LibellesPhase>> = {
  entrainement: { bandeau: 'Phase d’entraînement', zoneDeJeu: 'Terrain d’entraînement' },
  avancee: { bandeau: 'Avancée des monstres', zoneDeJeu: 'Champ de bataille' },
  combat: { bandeau: 'Phase de combat', zoneDeJeu: 'Champ de bataille' },
  boss: { bandeau: 'Combat de boss', zoneDeJeu: 'Champ de bataille' },
};
