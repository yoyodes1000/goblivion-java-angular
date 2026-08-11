/**
 * La forme des données de cartes, telle que décrite dans docs/modele-cartes.md.
 *
 * Les données elles-mêmes sont du contenu Goblivion Games et ne sont pas
 * versionnées : ce fichier ne décrit que leur structure. Les énumérations sont
 * en majuscules pour se transposer telles quelles en `enum` Java le jour où le
 * backend les lira.
 */

export type TypeCarte = 'HUMAIN' | 'OBJET';

/**
 * Deux cartes ne portent pas de force imprimée — le Soldat et le Joker. Pour
 * elles `force` vaut `null` et c'est cette règle-là qui s'applique. Une carte a
 * toujours l'un ou l'autre, jamais les deux.
 */
export type ForceVariable = 'SOLDAT' | 'JOKER';

/** Les familles de cartes, qui sont aussi les dossiers de scans. */
export type Famille = 'bleues' | 'boss' | 'dorees' | 'ennemis-objets' | 'roi-reines';

export interface ProcessusEntrainement {
  /** Cartes à piocher pour tenter l'entraînement. */
  readonly pioche: number;
  /** Force à réunir. */
  readonly valeur: number;
  /** Type de carte à détruire pour conclure. */
  readonly sacrifice: TypeCarte;
}

export interface CarteBleue {
  readonly id: string;
  readonly nom: string;
  readonly type: TypeCarte;
  readonly scan: string;
  readonly force: number | null;
  readonly forceVariable: ForceVariable | null;
  /** Toujours 0 pour les Bleues : elles ne s'achètent pas. */
  readonly niveau: number;
  readonly action: string | null;
  readonly exemplaires: number;
}

export interface CarteDoree {
  readonly id: string;
  readonly nom: string;
  readonly type: TypeCarte;
  readonly scan: string;
  readonly force: number | null;
  readonly forceVariable: ForceVariable | null;
  /** 1 ou 2 épées : les 2 épées ne s'ouvrent qu'après un premier combat gagné. */
  readonly niveau: number;
  readonly action: string | null;
  readonly entrainement: ProcessusEntrainement;
  readonly exemplaires: number;
}

export interface RoiReine {
  readonly id: string;
  readonly nom: string;
  readonly scan: string;
  /** Montant de départ des ressources — 16 à 21 selon le rôle. Pas un plafond. */
  readonly ressourcesDepart: number;
  /** `id` d'une carte de `dorees.json` : le Garde du corps initial. */
  readonly gardeDuCorps: string;
  readonly action: string;
}

/**
 * Ce qu'il faut savoir d'une carte pour la montrer, quelle que soit sa famille.
 *
 * L'hôpital mélange des Bleues et des Dorées : l'affichage n'a pas besoin d'en
 * savoir plus que le nom et le scan, et n'a donc pas à connaître les deux types
 * complets.
 */
export interface CarteAffichable {
  readonly id: string;
  readonly nom: string;
  readonly scan: string;
  readonly famille: Famille;
}

/**
 * Une seule carte physique, deux moitiés tête-bêche : d'où un objet porteur et
 * deux sous-objets plutôt que deux cartes. `exemplaires` porte sur la carte
 * entière — deux Gobelins Assassins, c'est aussi deux Lames Toxiques.
 */
export interface CarteEnnemiObjet {
  readonly id: string;
  readonly scan: string;
  readonly exemplaires: number;
  readonly ennemi: {
    readonly nom: string;
    readonly niveau: number;
    readonly pioche: number;
    readonly force: number | null;
    readonly action: string | null;
  };
  readonly objet: {
    readonly nom: string;
    readonly type: TypeCarte;
    readonly force: number | null;
    readonly forceVariable: ForceVariable | null;
    readonly action: string | null;
  };
}
