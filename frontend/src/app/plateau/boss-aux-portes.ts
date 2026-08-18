/**
 * Le Boss affronté, tel qu'il se montre aux Portes.
 *
 * Il y prend la place des trois cases : quand la phase de Boss s'ouvre, le
 * château brûle et les ennemis qui s'y tenaient sont détruits (§10). L'endroit
 * est donc libre, et c'est le même : ce qui se dresse devant les Portes est ce
 * qu'on affronte.
 *
 * `force` et `pioche` viennent du catalogue, pas de l'API — ce sont des valeurs
 * imprimées sur la carte, qu'aucun jeton ne modifie : « Un Boss ne gagne jamais
 * de jeton bonus » (§10.4).
 */
export interface BossAuxPortes {
  readonly nom: string;
  readonly image: string;
  /** La force à égaler pour le vaincre — le seuil est inclusif (§8). */
  readonly force: number;
  /** Les cartes que son assaut fait piocher. */
  readonly pioche: number;
  readonly action: string | null;
  /** Vrai entre l'assaut et sa résolution : les cartes sont tombées, à toi de jouer. */
  readonly assautEngage: boolean;
  /** Combien de Boss restent à abattre, celui-ci compris. */
  readonly restants: number;
}
